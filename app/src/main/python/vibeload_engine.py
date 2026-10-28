import base64
import http.cookiejar
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.parse
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

def clean_and_normalize_url(raw_url):
    """
    Clean and normalize media URL before passing to yt-dlp:
    1. Strip leading/trailing whitespaces and quotation marks.
    2. Normalize Threads domains: threads.com -> threads.net.
    3. Strip non-essential tracking queries across platforms (Meta, TikTok, YouTube).
    """
    if not raw_url:
        return ""
    url = raw_url.strip().strip('"\'')

    # Fix Threads domain (threads.com -> threads.net)
    if "threads.com" in url.lower():
        url = re.sub(r'https?://(?:www\.)?threads\.com', 'https://www.threads.net', url, flags=re.IGNORECASE)

    # Strip non-essential tracking queries
    try:
        parsed = urllib.parse.urlparse(url)
        if parsed.query:
            tracking_params = {
                'xmt', 'slof', 'mibextid', 'fbclid', 'igsh', 'igshid',
                'rdid', 'share_id', '_r', '_t', 'is_from_webapp',
                'sender_device', 'sender_web_id', 'si', 'feature'
            }
            query_pairs = urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
            cleaned_pairs = [
                (k, v) for k, v in query_pairs 
                if k.lower() not in tracking_params and not k.lower().startswith('utm_')
            ]
            new_query = urllib.parse.urlencode(cleaned_pairs)
            parsed = parsed._replace(query=new_query)
            url = urllib.parse.urlunparse(parsed)
    except Exception:
        pass

    return url

def resolve_redirect_if_needed(url):
    """
    Follow HTTP redirects for short links and share wrappers:
    - TikTok: vt.tiktok.com, vm.tiktok.com
    - Facebook: fb.watch, facebook.com/share/
    """
    if not url:
        return ""
    url_lower = url.lower()
    if any(k in url_lower for k in ("vt.tiktok.com", "vm.tiktok.com", "fb.watch", "facebook.com/share/")):
        try:
            req = urllib.request.Request(url, headers=DEFAULT_HEADERS)
            with urllib.request.urlopen(req, timeout=10) as resp:
                resolved = resp.geturl()
                if resolved:
                    # Guard for TikTok: if it redirected to the home page or root path,
                    # DO NOT use it! Keep the original short URL so TikWM/extractors can parse it.
                    if "vt.tiktok.com" in url_lower or "vm.tiktok.com" in url_lower:
                        parsed = urllib.parse.urlparse(resolved)
                        if parsed.path in ("", "/") or "login" in resolved.lower():
                            return url
                    return resolved
        except Exception:
            pass
    return url

def format_error_message(err_str, url=""):
    """
    Translate raw yt-dlp CLI errors into polite, user-friendly Vietnamese guidance.
    Eliminates confusing CLI flags like '--cookies-from-browser' and internal stack traces.
    """
    err_lower = (err_str or "").lower()
    url_lower = (url or "").lower()

    # 1. YouTube Bot Detection / Security Challenge
    if "confirm you're not a bot" in err_lower or "not a bot" in err_lower:
        return "YouTube yêu cầu xác minh bảo mật hoặc mạng hiện tại đang bị hạn chế tạm thời. Vui lòng thử chuyển sang mạng 4G/Wi-Fi khác hoặc thử lại sau vài phút."

    # 2. Instagram Story / Private / Cookies Required
    if "instagram:story" in err_lower or "cookies-from-browser" in err_lower or ("instagram" in url_lower and ("unreachable" in err_lower or "cookie" in err_lower or "login" in err_lower)):
        return "Tin (Story) hoặc bài viết Instagram này yêu cầu đăng nhập tài khoản để xem. Vui lòng thử tải các bài viết hoặc Reels công khai."

    # 3. TikTok Photo / Slideshow Album / Content
    if "/photo/" in url_lower or ("tiktok" in url_lower and ("photo" in err_lower or "unsupported url" in err_lower)):
        return "Không thể tải nội dung TikTok này. Bài viết có thể ở chế độ riêng tư, đã bị xóa hoặc TikTok đang yêu cầu xác minh bảo mật."

    # 4. Facebook Private / Closed Group / Login Wall
    if "cannot parse data" in err_lower or ("facebook" in url_lower and ("login" in err_lower or "unsupported url" in err_lower or "report this issue" in err_lower)):
        return "Không thể lấy dữ liệu video từ Facebook. Bài viết có thể thuộc nhóm kín, ở chế độ riêng tư hoặc Facebook đang yêu cầu đăng nhập tài khoản."

    # 5. Threads Invalid / Expired / Private Post
    if "threads" in url_lower and ("unsupported url" in err_lower or "invalid_post" in err_lower or "404" in err_lower):
        return "Bài viết trên Threads không tồn tại, đã bị xóa hoặc thuộc tài khoản riêng tư."

    # 6. Video Unavailable / Deleted / Region Restricted
    if "this video is unavailable" in err_lower or "video unavailable" in err_lower:
        return "Video này hiện không khả dụng (đã bị xóa, ở chế độ riêng tư hoặc bị chặn tại quốc gia của bạn)."

    # 7. General Private Content / Permission Denied
    if "private video" in err_lower or "permission" in err_lower or "login required" in err_lower:
        return "Nội dung này được đặt ở chế độ riêng tư hoặc yêu cầu quyền truy cập từ tác giả."

    # 8. Rate Limiting / IP Blocked / HTTP 429
    if "blocked" in err_lower or "too many requests" in err_lower or "429" in err_lower:
        return "Địa chỉ mạng hiện tại tạm thời bị nền tảng giới hạn tần suất yêu cầu (Too Many Requests). Vui lòng đổi sang mạng 4G/Wi-Fi khác hoặc thử lại sau ít phút."

    # 9. Age-Restricted / Member-Only
    if "age-restricted" in err_lower or "members-only" in err_lower or "join this channel" in err_lower:
        return "Video này giới hạn độ tuổi hoặc chỉ dành riêng cho hội viên trả phí của kênh."

    # 10. Timeout / Network Connection Issue
    if "timed out" in err_lower or "timeout" in err_lower or "connection reset" in err_lower:
        return "Kết nối mạng tới máy chủ bị quá thời gian chờ (Timeout). Vui lòng kiểm tra lại kết nối Internet."

    # 11. Unsupported URL
    if "unsupported url" in err_lower:
        return "Liên kết không được hỗ trợ hoặc không đúng định dạng. Vui lòng kiểm tra lại liên kết."

    # General fallback cleanup
    clean_err = re.sub(r'\[\w+\]\s*[^:]*:\s*', '', err_str).strip()
    clean_err = re.sub(r'please report this issue.*', '', clean_err, flags=re.IGNORECASE).strip()
    return clean_err if clean_err else "Không thể phân tích liên kết này."

def extract_tiktok_via_tikwm(url):
    """
    Secondary fallback for TikTok photo carousels/slides, videos, and music via TikWM API.
    """
    endpoints = ["https://www.tikwm.com/api/", "https://tikwm.com/api/"]
    for ep in endpoints:
        try:
            data = urllib.parse.urlencode({"url": url, "hd": 1}).encode("utf-8")
            req = urllib.request.Request(
                ep,
                data=data,
                headers={
                    "User-Agent": DEFAULT_HEADERS["User-Agent"],
                    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                    "Accept": "application/json, text/javascript, */*; q=0.01"
                }
            )
            with urllib.request.urlopen(req, timeout=12) as resp:
                res = json.loads(resp.read().decode("utf-8"))
                if res.get("code") == 0 and "data" in res:
                    d = res["data"]
                    images = d.get("images", [])
                    music_url = d.get("music") or d.get("play")

                    if not images:
                        # Fallback for TikTok Video or TikTok Music when yt-dlp is restricted
                        video_url = d.get("hdplay") or d.get("play")
                        cover = d.get("cover") or ""
                        duration = d.get("duration") or 0
                        video_options = []
                        if video_url:
                            video_options.append({
                                "format_id": video_url,
                                "resolution": "HD (Không logo)",
                                "height": 1080,
                                "width": 1920,
                                "ext": "mp4",
                                "filesize": d.get("hd_size") or d.get("size") or 0,
                                "vcodec": "h264",
                                "acodec": "aac",
                                "needs_merge": False
                            })
                        audio_options = []
                        if music_url:
                            audio_options.append({
                                "format_id": music_url,
                                "ext": "mp3",
                                "abr": 128,
                                "filesize": 0,
                                "acodec": "mp3"
                            })
                        is_audio_only = "/music/" in url.lower() or (len(video_options) == 0 and len(audio_options) > 0)
                        return {
                            "success": True,
                            "id": str(d.get("id") or ""),
                            "url": url,
                            "title": d.get("title") or "TikTok",
                            "thumbnail": cover,
                            "uploader": d.get("author", {}).get("nickname") or d.get("author", {}).get("unique_id") or "TikTok",
                            "duration": duration,
                            "platform": "tiktok",
                            "video_options": video_options if not is_audio_only else [],
                            "audio_options": audio_options,
                            "image_options": [],
                            "is_image_only": False,
                            "is_audio_only": is_audio_only
                        }

                    title = d.get("title") or "TikTok Ảnh"
                    author = d.get("author", {}).get("nickname") or d.get("author", {}).get("unique_id") or "TikTok"

                    image_options = []
                    for idx, img_url in enumerate(images):
                        image_options.append({
                            "format_id": img_url,
                            "label": f"Ảnh {idx + 1}/{len(images)}",
                            "ext": "jpg",
                            "resolution": f"Ảnh {idx + 1}",
                            "filesize": 0,
                            "is_image": True,
                            "mime_type": "image/jpeg"
                        })

                    audio_options = []
                    if music_url:
                        audio_options.append({
                            "format_id": music_url,
                            "ext": "mp3",
                            "abr": 128,
                            "filesize": 0,
                            "acodec": "mp3"
                        })

                    item_id = str(d.get("id") or "")
                    return {
                        "success": True,
                        "id": item_id,
                        "url": url,
                        "title": title,
                        "thumbnail": image_options[0]["format_id"] if image_options else "",
                        "uploader": author,
                        "duration": 0,
                        "platform": "tiktok",
                        "video_options": [],
                        "audio_options": audio_options,
                        "image_options": image_options,
                        "is_image_only": True
                    }
        except Exception:
            pass
    return None

def extract_tiktok_photos_direct(url):
    """
    Extract photo slideshow images and background music from TikTok photo posts via direct HTML parsing.
    """
    try:
        cj = http.cookiejar.CookieJar()
        opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
        req = urllib.request.Request(url, headers=DEFAULT_HEADERS)
        with opener.open(req, timeout=15) as resp:
            final_url = resp.geturl()
            html = resp.read().decode('utf-8', errors='ignore')

        item = {}
        # Try 1: __UNIVERSAL_DATA_FOR_REHYDRATION__
        m = re.search(r'<script\s+id=[\"\']__UNIVERSAL_DATA_FOR_REHYDRATION__[\"\'][^>]*>(.*?)</script>', html, re.DOTALL)
        if m:
            try:
                data = json.loads(m.group(1))
                scope = data.get('__DEFAULT_SCOPE__', {})
                for k, v in scope.items():
                    if isinstance(v, dict):
                        cand = v.get('itemInfo', {}).get('itemStruct') or v.get('itemStruct') or v.get('photoInfo') or v
                        if isinstance(cand, dict) and ('imagePost' in cand or 'images' in cand):
                            item = cand
                            break
                if not item:
                    item = scope.get('webapp.video-detail', {}).get('itemInfo', {}).get('itemStruct', {})
            except Exception:
                pass

        # Try 2: SIGI_STATE
        if not item:
            m2 = re.search(r'<script\s+id=[\"\']SIGI_STATE[\"\'][^>]*>(.*?)</script>', html, re.DOTALL)
            if m2:
                try:
                    data = json.loads(m2.group(1))
                    item_module = data.get('ItemModule', {})
                    if item_module:
                        item = next(iter(item_module.values()))
                except Exception:
                    pass

        image_post = item.get('imagePost', {}) if isinstance(item, dict) else {}
        images = image_post.get('images', []) or (item.get('images', []) if isinstance(item, dict) else [])

        if not images:
            return None

        item_id = str(item.get("id") or "")
        desc = item.get('desc') or 'TikTok Ảnh'
        author = item.get('author', {}).get('nickname') or item.get('author', {}).get('uniqueId') or 'TikTok'

        image_options = []
        for idx, img in enumerate(images):
            if isinstance(img, str):
                best_img_url = img
            else:
                url_list = img.get('imageURL', {}).get('urlList', []) or img.get('urlList', [])
                if not url_list:
                    continue
                best_img_url = url_list[0]
            image_options.append({
                "format_id": best_img_url,
                "label": f"Ảnh {idx + 1}/{len(images)}",
                "ext": "jpg",
                "resolution": f"Ảnh {idx + 1}",
                "filesize": 0,
                "is_image": True,
                "mime_type": "image/jpeg"
            })

        audio_options = []
        music = item.get('music', {}) if isinstance(item, dict) else {}
        play_url = music.get('playUrl')
        if play_url:
            audio_options.append({
                "format_id": play_url,
                "ext": "mp3",
                "abr": 128,
                "filesize": 0,
                "acodec": "mp3"
            })

        first_thumb = image_options[0]["format_id"] if image_options else ""

        return {
            "success": True,
            "id": item_id,
            "url": final_url,
            "title": desc,
            "thumbnail": first_thumb,
            "uploader": author,
            "duration": 0,
            "platform": "tiktok",
            "video_options": [],
            "audio_options": audio_options,
            "image_options": image_options,
            "is_image_only": True
        }
    except Exception:
        return None

def extract_tiktok_photos(url):
    """
    Extract photo slideshow images or video/music from TikTok posts.
    Tries TikWM fallback first for shortlinks (vt.tiktok.com/vm.tiktok.com),
    and direct HTML parsing first for full URLs.
    """
    url_lower = url.lower()
    if "vt.tiktok.com" in url_lower or "vm.tiktok.com" in url_lower:
        tikwm_res = extract_tiktok_via_tikwm(url)
        if tikwm_res:
            return tikwm_res
        direct_res = extract_tiktok_photos_direct(url)
        if direct_res:
            return direct_res
        return None

    direct_res = extract_tiktok_photos_direct(url)
    if direct_res:
        return direct_res
    return extract_tiktok_via_tikwm(url)

def extract_opengraph_media(url):
    """
    Fallback extractor for platforms (Facebook photos, articles, web pages)
    using OpenGraph metadata when yt-dlp fails or is unsupported.
    """
    try:
        headers = dict(DEFAULT_HEADERS)
        if "facebook" in url.lower() or "fb.watch" in url.lower() or "/share/" in url.lower():
            headers["User-Agent"] = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=12) as resp:
            final_url = resp.geturl()
            html = resp.read().decode('utf-8', errors='ignore')

        # Check for og:image
        og_img_match = re.search(r'<meta\s+(?:property|name)=[\"\']og:image[\"\']\s+content=[\"\']([^\"\']+)[\"\']', html, re.I)
        if not og_img_match:
            og_img_match = re.search(r'<meta\s+content=[\"\']([^\"\']+)[\"\']\s+(?:property|name)=[\"\']og:image[\"\']', html, re.I)

        if not og_img_match:
            return None

        img_url = og_img_match.group(1).replace('&amp;', '&')
        if not img_url.startswith('http'):
            return None

        # Filter out Facebook logos or placeholder icons
        if "facebook_logo" in img_url.lower() or "static.xx.fbcdn.net/rsrc.php" in img_url.lower():
            return None

        # Check for og:title
        title = "Ảnh phương tiện"
        t_match = re.search(r'<meta\s+(?:property|name)=[\"\']og:title[\"\']\s+content=[\"\']([^\"\']+)[\"\']', html, re.I)
        if not t_match:
            t_match = re.search(r'<meta\s+content=[\"\']([^\"\']+)[\"\']\s+(?:property|name)=[\"\']og:title[\"\']', html, re.I)
        if t_match:
            title = t_match.group(1).replace('&amp;', '&').replace('&quot;', '"').strip()

        # Check for og:site_name
        site_name = "Facebook" if "facebook" in url.lower() else "Mạng xã hội"
        s_match = re.search(r'<meta\s+(?:property|name)=[\"\']og:site_name[\"\']\s+content=[\"\']([^\"\']+)[\"\']', html, re.I)
        if s_match:
            site_name = s_match.group(1).strip()

        ext = "jpg"
        for candidate_ext in ["png", "webp", "gif", "jpeg", "jpg"]:
            if f".{candidate_ext}" in img_url.lower():
                ext = "jpg" if candidate_ext == "jpeg" else candidate_ext
                break

        return {
            "success": True,
            "url": final_url,
            "title": title,
            "thumbnail": img_url,
            "uploader": site_name,
            "duration": 0,
            "platform": "facebook" if "facebook" in url.lower() else "web",
            "video_options": [],
            "audio_options": [],
            "image_options": [{
                "format_id": img_url,
                "label": f"Ảnh ({ext.upper()})",
                "ext": ext,
                "resolution": "Gốc",
                "filesize": 0,
                "is_image": True,
                "mime_type": f"image/{ext}"
            }],
            "is_image_only": True
        }
    except Exception:
        return None

def analyze_url(url):
    """
    Analyze given media URL and return structured JSON metadata.
    Supports YouTube, TikTok, Facebook, Instagram, SoundCloud, etc.
    """
    clean_url = clean_and_normalize_url(url)
    clean_url = resolve_redirect_if_needed(clean_url)

    # For TikTok photo posts, shortlinks, or music links, attempt specialized extractor first
    if "/photo/" in clean_url.lower() or "vt.tiktok.com" in clean_url.lower() or "vm.tiktok.com" in clean_url.lower() or "/music/" in clean_url.lower() or "vt.tiktok.com" in url.lower() or "vm.tiktok.com" in url.lower():
        tiktok_photo_res = extract_tiktok_photos(clean_url)
        if not tiktok_photo_res and url != clean_url:
            tiktok_photo_res = extract_tiktok_photos(url)
        if tiktok_photo_res:
            return json.dumps(tiktok_photo_res)

    target_url = clean_url
    if "/photo/" in clean_url.lower():
        target_url = re.sub(r'/photo/(\d+)', r'/video/\1', clean_url)

    ffmpeg_loc = find_ffmpeg_location()
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'skip_download': True,
        'extract_flat': False,
        'socket_timeout': 20,
        'noplaylist': True, # Enforce 1-video per request (FR-01)
        'http_headers': DEFAULT_HEADERS,
        'extractor_args': {
            'youtube': {
                'player_client': ['android', 'ios', 'web'],
                'player_skip': ['configs', 'webpage']
            }
        }
    }
    if ffmpeg_loc:
        ydl_opts['ffmpeg_location'] = ffmpeg_loc

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            try:
                info = ydl.extract_info(target_url, download=False)
            except Exception as initial_err:
                if target_url != clean_url:
                    info = ydl.extract_info(clean_url, download=False)
                else:
                    raise initial_err

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
                "audio_options": audio_options,
                "is_audio_only": len(video_options) == 0 and len(audio_options) > 0
            }
            return json.dumps(result)

    except yt_dlp.utils.DownloadError as e:
        if "tiktok" in clean_url.lower() or "vt.tiktok.com" in clean_url.lower() or "tiktok" in url.lower() or "vt.tiktok.com" in url.lower():
            tt_photos = extract_tiktok_photos(clean_url)
            if not tt_photos and url != clean_url:
                tt_photos = extract_tiktok_photos(url)
            if tt_photos:
                return json.dumps(tt_photos)
        if "facebook" in clean_url.lower() or "fb.watch" in clean_url.lower() or "/share/" in clean_url.lower() or "facebook" in url.lower():
            fb_media = extract_opengraph_media(clean_url)
            if not fb_media and url != clean_url:
                fb_media = extract_opengraph_media(url)
            if fb_media:
                return json.dumps(fb_media)
        return json.dumps({"success": False, "error": format_error_message(str(e), url)})
    except Exception as e:
        if "tiktok" in clean_url.lower() or "vt.tiktok.com" in clean_url.lower() or "tiktok" in url.lower() or "vt.tiktok.com" in url.lower():
            tt_photos = extract_tiktok_photos(clean_url)
            if not tt_photos and url != clean_url:
                tt_photos = extract_tiktok_photos(url)
            if tt_photos:
                return json.dumps(tt_photos)
        if "facebook" in clean_url.lower() or "fb.watch" in clean_url.lower() or "/share/" in clean_url.lower() or "facebook" in url.lower():
            fb_media = extract_opengraph_media(clean_url)
            if not fb_media and url != clean_url:
                fb_media = extract_opengraph_media(url)
            if fb_media:
                return json.dumps(fb_media)
        return json.dumps({"success": False, "error": format_error_message(str(e), url)})

def download_stream(url, format_spec, output_path, progress_listener=None):
    """
    Download given stream into output_path with progress hook.
    progress_listener is a Python callable or Chaquopy Java proxy with onProgress(...) and isCancelled() -> bool.
    """
    clean_url = clean_and_normalize_url(url)

    # Direct URL download (e.g. TikTok photo or audio CDN URL)
    if format_spec.startswith("http://") or format_spec.startswith("https://"):
        try:
            req = urllib.request.Request(format_spec, headers=DEFAULT_HEADERS)
            with urllib.request.urlopen(req, timeout=30) as resp:
                total_bytes = int(resp.headers.get('Content-Length', 0))
                downloaded = 0
                with open(output_path, 'wb') as out_f:
                    while True:
                        if progress_listener and hasattr(progress_listener, "isCancelled") and progress_listener.isCancelled():
                            raise Exception("DOWNLOAD_CANCELLED")
                        chunk = resp.read(65536)
                        if not chunk:
                            break
                        out_f.write(chunk)
                        downloaded += len(chunk)
                        if progress_listener and hasattr(progress_listener, "onProgress") and total_bytes > 0:
                            pct = (downloaded / total_bytes) * 100.0
                            progress_listener.onProgress(float(pct), 0.0, int(downloaded), int(total_bytes), 0)
            return json.dumps({"success": True, "output_path": output_path})
        except Exception as dl_err:
            return json.dumps({"success": False, "error": str(dl_err)})

    target_url = clean_url
    if "/photo/" in clean_url.lower():
        target_url = re.sub(r'/photo/(\d+)', r'/video/\1', clean_url)

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
        'extractor_args': {
            'youtube': {
                'player_client': ['android', 'ios', 'web'],
                'player_skip': ['configs', 'webpage']
            }
        }
    }

    if ffmpeg_loc:
        ydl_opts['ffmpeg_location'] = ffmpeg_loc
        if '+' in format_spec:
            ydl_opts['merge_output_format'] = 'mp4'

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([target_url])

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
        return json.dumps({"success": False, "cancelled": False, "error": format_error_message(str(e), url)})

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
