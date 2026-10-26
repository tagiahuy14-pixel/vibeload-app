import base64
import json
import os
import shutil
import subprocess
import sys
import time
import yt_dlp

DEFAULT_HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9,vi;q=0.8',
    'Sec-Fetch-Mode': 'navigate',
}

def find_ffmpeg_location():
    """Detect if FFmpeg is installed and return its directory."""
    which_ffmpeg = shutil.which("ffmpeg")
    if which_ffmpeg:
        return os.path.dirname(which_ffmpeg)

    # Check common Windows locations
    localappdata = os.environ.get("LOCALAPPDATA", "")
    if localappdata:
        winget_dir = os.path.join(localappdata, "Microsoft", "WinGet", "Packages")
        if os.path.exists(winget_dir):
            for root, dirs, files in os.walk(winget_dir):
                if "ffmpeg.exe" in files:
                    return root
    return None

def find_ffprobe_path():
    """Find ffprobe binary path if available."""
    which_ffprobe = shutil.which("ffprobe")
    if which_ffprobe:
        return which_ffprobe

    ffmpeg_dir = find_ffmpeg_location()
    if ffmpeg_dir:
        cand = os.path.join(ffmpeg_dir, "ffprobe.exe" if sys.platform == "win32" else "ffprobe")
        if os.path.exists(cand):
            return cand
    return None

def get_engine_version():
    """Return version info for yt-dlp, FFmpeg and python environment."""
    ffmpeg_loc = find_ffmpeg_location()
    return {
        "yt_dlp_version": yt_dlp.version.__version__,
        "python_version": sys.version,
        "platform": sys.platform,
        "ffmpeg_location": ffmpeg_loc or "None (Using in-process FFmpegKit)",
        "has_external_ffmpeg": ffmpeg_loc is not None
    }

def analyze_url(url):
    """
    Analyze given media URL and return structured JSON metadata.
    Supports YouTube, TikTok, Facebook, Instagram, SoundCloud, etc.
    """
    ffmpeg_loc = find_ffmpeg_location()
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'skip_download': True,
        'extract_flat': False,
        'socket_timeout': 20,
        'noplaylist': True, # Enforce 1-video per request (FR-01)
        'http_headers': DEFAULT_HEADERS,
    }
    if ffmpeg_loc:
        ydl_opts['ffmpeg_location'] = ffmpeg_loc

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                return json.dumps({"success": False, "error": "Không tìm thấy dữ liệu phương tiện từ liên kết này."})

            # Handle playlist links if encountered (FR-01)
            if 'entries' in info and info['entries']:
                info = info['entries'][0]

            title = info.get('title') or info.get('description') or 'Unknown Title'
            thumbnail = info.get('thumbnail', '')
            uploader = info.get('uploader') or info.get('uploader_id') or info.get('channel', 'Unknown Author')
            duration = info.get('duration', 0)
            extractor = (info.get('extractor') or 'unknown').lower()

            raw_formats = info.get('formats', [])
            video_options = []
            audio_options = []
            seen_resolutions = set()

            for fmt in raw_formats:
                fmt_id = str(fmt.get('format_id', ''))
                ext = fmt.get('ext', '')
                vcodec = fmt.get('vcodec') or 'none'
                acodec = fmt.get('acodec') or 'none'
                height = fmt.get('height') or 0
                width = fmt.get('width') or 0
                filesize = fmt.get('filesize') or fmt.get('filesize_approx') or 0
                abr = fmt.get('abr') or 0

                # Skip storyboard or mhtml formats
                if fmt_id.startswith('sb') or ext in ('mhtml',) or fmt.get('protocol') == 'mhtml':
                    continue

                has_vcodec = vcodec != 'none' and vcodec is not None
                has_acodec = acodec != 'none' and acodec is not None

                # Special resolution handling for Facebook & platforms with named formats (sd/hd)
                if fmt_id == 'hd' and height == 0:
                    height = 720
                elif fmt_id == 'sd' and height == 0:
                    height = 480

                # Formats like Facebook (sd, hd) or TikTok combined formats have both audio & video
                is_named_video = fmt_id in ('sd', 'hd', 'video')
                is_video = has_vcodec or is_named_video
                is_audio_only = has_acodec and not has_vcodec and not is_named_video

                if is_video and height > 0:
                    res_label = f"{height}p"
                    if fmt_id in ('sd', 'hd'):
                        res_label = f"{fmt_id.upper()} ({height}p)"

                    needs_merge = not has_acodec and fmt_id not in ('sd', 'hd') and ('tiktok' not in extractor)
                    
                    if res_label not in seen_resolutions:
                        seen_resolutions.add(res_label)
                        video_options.append({
                            "format_id": fmt_id,
                            "resolution": res_label,
                            "height": height,
                            "width": width,
                            "ext": "mp4",
                            "filesize": filesize,
                            "vcodec": vcodec,
                            "acodec": acodec,
                            "needs_merge": needs_merge
                        })
                    else:
                        for existing_opt in video_options:
                            if existing_opt["resolution"] == res_label:
                                # Priority 1: Format that already has audio included (no merge needed)
                                if existing_opt["needs_merge"] and not needs_merge:
                                    existing_opt["format_id"] = fmt_id
                                    existing_opt["vcodec"] = vcodec
                                    existing_opt["acodec"] = acodec
                                    existing_opt["needs_merge"] = False
                                    if filesize > 0:
                                        existing_opt["filesize"] = filesize
                                    break
                                # Priority 2: Prefer H.264 (avc1) stream for flawless MP4 fast-copy without transcoding
                                elif existing_opt["needs_merge"] and needs_merge:
                                    cur_is_avc = "avc" in vcodec.lower() or "h264" in vcodec.lower()
                                    prev_is_avc = "avc" in existing_opt["vcodec"].lower() or "h264" in existing_opt["vcodec"].lower()
                                    if cur_is_avc and not prev_is_avc:
                                        existing_opt["format_id"] = fmt_id
                                        existing_opt["vcodec"] = vcodec
                                        existing_opt["acodec"] = acodec
                                        if filesize > 0:
                                            existing_opt["filesize"] = filesize
                                        break

                elif is_audio_only:
                    audio_options.append({
                        "format_id": fmt_id,
                        "ext": ext,
                        "abr": int(abr) if abr else 128,
                        "filesize": filesize,
                        "acodec": acodec
                    })

            # Sort video options by height ascending
            video_options.sort(key=lambda x: x["height"])

            # Fallback if no video options found, only if a valid video format exists
            if not video_options and raw_formats:
                candidate_fmts = [f for f in raw_formats if (f.get('vcodec') or 'none') != 'none' or str(f.get('format_id')) in ('sd', 'hd', 'video')]
                if candidate_fmts:
                    last_fmt = candidate_fmts[-1]
                    vcodec = last_fmt.get('vcodec') or 'none'
                    acodec = last_fmt.get('acodec') or 'none'
                    needs_merge = (vcodec != 'none') and (acodec == 'none')
                    h = last_fmt.get('height') or 720
                    video_options.append({
                        "format_id": str(last_fmt.get('format_id', 'best')),
                        "resolution": f"{h}p (Mặc định)",
                        "height": h,
                        "width": last_fmt.get('width') or 1280,
                        "ext": "mp4",
                        "filesize": last_fmt.get('filesize') or 0,
                        "vcodec": vcodec,
                        "acodec": acodec,
                        "needs_merge": needs_merge
                    })

            result = {
                "success": True,
                "url": url,
                "title": title,
                "thumbnail": thumbnail,
                "uploader": uploader,
                "duration": duration,
                "platform": extractor,
                "video_options": video_options,
                "audio_options": audio_options
            }
            return json.dumps(result)

    except yt_dlp.utils.DownloadError as e:
        err_msg = str(e)
        if "blocked" in err_msg.lower():
            err_msg = "Địa chỉ IP hoặc thiết bị bị chặn bởi nền tảng. Vui lòng thử lại sau."
        elif "login" in err_msg.lower() or "private" in err_msg.lower():
            err_msg = "Nội dung này là riêng tư hoặc yêu cầu đăng nhập."
        return json.dumps({"success": False, "error": f"Lỗi phân tích: {err_msg}"})
    except Exception as e:
        return json.dumps({"success": False, "error": f"Lỗi hệ thống: {str(e)}"})

def download_stream(url, format_spec, output_path, progress_listener=None):
    """
    Download given stream into output_path with progress hook.
    progress_listener is a Python callable or Chaquopy Java proxy with onProgress(...) and isCancelled() -> bool.
    """
    def hook(d):
        if progress_listener:
            try:
                if hasattr(progress_listener, "isCancelled") and progress_listener.isCancelled():
                    raise Exception("DOWNLOAD_CANCELLED")
            except Exception as check_err:
                if "DOWNLOAD_CANCELLED" in str(check_err):
                    raise check_err

            status = d.get('status')
            if status == 'downloading':
                downloaded = d.get('downloaded_bytes', 0)
                total = d.get('total_bytes') or d.get('total_bytes_estimate') or 0
                speed = d.get('speed') or 0.0
                eta = d.get('eta') or 0
                percent = (downloaded / total * 100.0) if total > 0 else 0.0

                if hasattr(progress_listener, "onProgress"):
                    try:
                        progress_listener.onProgress(float(percent), float(speed), int(downloaded), int(total), int(eta))
                    except Exception:
                        pass

    ffmpeg_loc = find_ffmpeg_location()

    # Intelligent format selector fallback
    # If format_spec is a single format_id, allow fallback to best if that format_id is expired
    clean_format = format_spec
    if clean_format and '+' not in clean_format and not clean_format.endswith('/best'):
        clean_format = f"{clean_format}/best"

    ydl_opts = {
        'format': clean_format,
        'outtmpl': output_path,
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
        'socket_timeout': 30,
        'progress_hooks': [hook],
        'http_headers': DEFAULT_HEADERS,
    }

    if ffmpeg_loc:
        ydl_opts['ffmpeg_location'] = ffmpeg_loc
        if '+' in format_spec:
            ydl_opts['merge_output_format'] = 'mp4'

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])

        actual_path = output_path
        if not os.path.exists(output_path):
            parent_dir = os.path.dirname(output_path)
            base = os.path.basename(output_path)
            if os.path.exists(parent_dir):
                for f in os.listdir(parent_dir):
                    if f.startswith(base) and not f.endswith(".part"):
                        cand = os.path.join(parent_dir, f)
                        if os.path.isfile(cand) and os.path.getsize(cand) > 0:
                            shutil.move(cand, output_path)
                            actual_path = output_path
                            break

        return json.dumps({"success": True, "output_path": actual_path})
    except Exception as e:
        if "DOWNLOAD_CANCELLED" in str(e):
            return json.dumps({"success": False, "cancelled": True, "error": "Tác vụ đã bị hủy"})
        return json.dumps({"success": False, "cancelled": False, "error": str(e)})

def verify_file_streams(file_path):
    """
    Verify video and audio streams of the output media file using ffprobe if available.
    """
    if not os.path.exists(file_path) or os.path.getsize(file_path) == 0:
        return json.dumps({"success": False, "has_video": False, "has_audio": False, "error": "Tệp không tồn tại hoặc rỗng"})

    ffprobe = find_ffprobe_path()
    if not ffprobe:
        # If no standalone ffprobe on system (e.g. Android), fallback to size check
        return json.dumps({
            "success": True,
            "has_video": True,
            "has_audio": True,
            "file_size": os.path.getsize(file_path),
            "note": "ffprobe not present; verified via filesystem"
        })

    try:
        cmd = [ffprobe, "-v", "error", "-show_entries", "stream=codec_type,codec_name", "-of", "json", file_path]
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
        if res.returncode != 0:
            return json.dumps({"success": False, "error": f"ffprobe error: {res.stderr}"})

        data = json.loads(res.stdout)
        streams = data.get("streams", [])
        v = [s["codec_name"] for s in streams if s.get("codec_type") == "video"]
        a = [s["codec_name"] for s in streams if s.get("codec_type") == "audio"]

        return json.dumps({
            "success": True,
            "has_video": len(v) > 0,
            "has_audio": len(a) > 0,
            "video_codecs": v,
            "audio_codecs": a,
            "file_size": os.path.getsize(file_path)
        })
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})
