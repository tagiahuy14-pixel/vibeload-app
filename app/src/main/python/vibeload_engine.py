import json
import os
import sys
import time
import yt_dlp

def get_engine_version():
    """Return version info for yt-dlp and python environment."""
    return {
        "yt_dlp_version": yt_dlp.version.__version__,
        "python_version": sys.version,
        "platform": sys.platform
    }

def analyze_url(url):
    """
    Analyze given media URL and return structured JSON metadata.
    Supports YouTube, TikTok, Facebook, Instagram, SoundCloud, etc.
    """
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'skip_download': True,
        'extract_flat': False,
        'socket_timeout': 15,
        'noplaylist': True, # Enforce 1-video per request (FR-01)
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                return json.dumps({"success": False, "error": "EMPTY_METADATA"})

            # Handle playlist links if encountered (FR-01)
            if 'entries' in info and info['entries']:
                # Grab the first entry or report playlist
                info = info['entries'][0]

            title = info.get('title', 'Unknown Title')
            thumbnail = info.get('thumbnail', '')
            uploader = info.get('uploader') or info.get('uploader_id') or info.get('channel', 'Unknown Author')
            duration = info.get('duration', 0)
            extractor = info.get('extractor', 'unknown').lower()

            raw_formats = info.get('formats', [])
            video_options = []
            audio_options = []
            seen_resolutions = set()

            for fmt in raw_formats:
                fmt_id = str(fmt.get('format_id', ''))
                ext = fmt.get('ext', '')
                vcodec = fmt.get('vcodec', 'none')
                acodec = fmt.get('acodec', 'none')
                height = fmt.get('height') or 0
                width = fmt.get('width') or 0
                filesize = fmt.get('filesize') or fmt.get('filesize_approx') or 0
                tbr = fmt.get('tbr') or 0
                abr = fmt.get('abr') or 0

                is_video = vcodec != 'none' and vcodec is not None
                is_audio = acodec != 'none' and acodec is not None

                if is_video and height > 0:
                    res_label = f"{height}p"
                    # Add distinct resolution options (e.g. 360p, 480p, 720p, 1080p, 1440p, 2160p)
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
                            "needs_merge": not is_audio
                        })

                elif is_audio and not is_video:
                    audio_options.append({
                        "format_id": fmt_id,
                        "ext": ext,
                        "abr": int(abr) if abr else 128,
                        "filesize": filesize,
                        "acodec": acodec
                    })

            # Sort video options by height ascending
            video_options.sort(key=lambda x: x["height"])

            # If no separated video/audio found, try direct single format
            if not video_options and raw_formats:
                last_fmt = raw_formats[-1]
                video_options.append({
                    "format_id": str(last_fmt.get('format_id', 'best')),
                    "resolution": "Mặc định",
                    "height": last_fmt.get('height') or 720,
                    "width": last_fmt.get('width') or 1280,
                    "ext": "mp4",
                    "filesize": last_fmt.get('filesize') or 0,
                    "vcodec": last_fmt.get('vcodec', 'mp4'),
                    "acodec": last_fmt.get('acodec', 'aac'),
                    "needs_merge": False
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
        return json.dumps({"success": False, "error": f"DOWNLOAD_ERROR: {str(e)}"})
    except Exception as e:
        return json.dumps({"success": False, "error": f"SYSTEM_ERROR: {str(e)}"})

def download_stream(url, format_spec, output_path, progress_listener=None):
    """
    Download given stream into output_path with progress hook.
    progress_listener is a Python callable or Chaquopy Java proxy with onProgress(percent, speed, downloaded, total, eta)
    and isCancelled() -> bool.
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
                    progress_listener.onProgress(float(percent), float(speed), int(downloaded), int(total), int(eta))

    ydl_opts = {
        'format': format_spec,
        'outtmpl': output_path,
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
        'socket_timeout': 30,
        'progress_hooks': [hook],
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
        return json.dumps({"success": True, "output_path": output_path})
    except Exception as e:
        if "DOWNLOAD_CANCELLED" in str(e):
            return json.dumps({"success": False, "cancelled": True, "error": "Tác vụ đã bị hủy"})
        return json.dumps({"success": False, "cancelled": False, "error": str(e)})
