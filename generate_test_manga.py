#!/usr/bin/env python3
"""
MangaTV Reader - Test Sample Generator
Generates sample .cbz comic archives with ComicInfo.xml metadata and test page images.
"""

import os
import zipfile
from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "test_samples")
os.makedirs(OUTPUT_DIR, exist_ok=True)

def create_manga_page(page_num, total_pages, title, volume="Vol. 1"):
    # Create 1080x1528 comic scan page
    width, height = 1080, 1528
    image = Image.new("RGB", (width, height), color=(245, 245, 248))
    draw = ImageDraw.Draw(image)

    # Outer comic panel border
    draw.rectangle([40, 40, width - 40, height - 40], outline=(20, 20, 25), width=6)

    # Header
    draw.text((60, 60), f"{title} - {volume}", fill=(40, 40, 45))
    draw.text((width - 240, 60), f"Page {page_num} / {total_pages}", fill=(100, 100, 110))

    if page_num == 1:
        # Cover Page Design
        draw.rectangle([100, 200, width - 100, 900], fill=(20, 27, 45), outline=(0, 229, 255), width=8)
        draw.text((140, 350), title.upper(), fill=(0, 229, 255))
        draw.text((140, 450), f"OFFICIAL DIGITAL EDITION • {volume}", fill=(100, 255, 218))
        draw.text((140, 750), "MangaTV TV Reader Test Sample Archive", fill=(240, 246, 252))
    else:
        # Manga Panels
        draw.rectangle([80, 140, width - 80, 700], outline=(20, 20, 25), width=4, fill=(255, 255, 255))
        draw.text((120, 200), f"Panel 1: {title} Action Scene", fill=(30, 30, 30))

        draw.rectangle([80, 740, width // 2 - 20, height - 120], outline=(20, 20, 25), width=4, fill=(255, 255, 255))
        draw.text((100, 800), "Panel 2: Dialogue", fill=(30, 30, 30))

        draw.rectangle([width // 2 + 20, 740, width - 80, height - 120], outline=(20, 20, 25), width=4, fill=(255, 255, 255))
        draw.text((width // 2 + 40, 800), "Panel 3: Climax", fill=(30, 30, 30))

    # Page footer
    draw.text((width // 2 - 40, height - 80), f"- {page_num} -", fill=(80, 80, 90))

    return image

def create_comic_info_xml(title, series, number, summary, writer, penciller, page_count, manga="Yes"):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<ComicInfo xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <Title>{title}</Title>
  <Series>{series}</Series>
  <Number>{number}</Number>
  <Summary>{summary}</Summary>
  <Writer>{writer}</Writer>
  <Penciller>{penciller}</Penciller>
  <CoverArtist>{penciller}</CoverArtist>
  <PageCount>{page_count}</PageCount>
  <Manga>{manga}</Manga>
  <Publisher>MangaTV Digital</Publisher>
  <Year>2026</Year>
</ComicInfo>
"""

def generate_sample_cbz(filename, series_title, volume_num, num_pages=8):
    output_path = os.path.join(OUTPUT_DIR, filename)
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        # Add ComicInfo.xml
        xml_content = create_comic_info_xml(
            title=f"{series_title} - Volume {volume_num}",
            series=series_title,
            number=str(volume_num),
            summary=f"Follow the epic adventures in {series_title}! Specially formatted for 10-foot TV viewing on MangaTV Reader.",
            writer="Antigravity",
            penciller="Studio AGY",
            page_count=num_pages,
            manga="Yes"
        )
        zipf.writestr("ComicInfo.xml", xml_content)

        # Generate and write pages
        for i in range(1, num_pages + 1):
            img = create_manga_page(i, num_pages, series_title, f"Vol. {volume_num}")
            img_byte_arr = os.path.join(OUTPUT_DIR, f"temp_page_{i}.png")
            img.save(img_byte_arr, format="PNG")
            zipf.write(img_byte_arr, f"page_{i:03d}.png")
            os.remove(img_byte_arr)

    print(f"Generated sample archive: {output_path} ({os.path.getsize(output_path)} bytes)")
    return output_path

if __name__ == "__main__":
    generate_sample_cbz("sample_manga.cbz", "Cyber Samurai", 1, num_pages=10)
    generate_sample_cbz("sample_manga_vol2.cbz", "Cyber Samurai", 2, num_pages=8)
    generate_sample_cbz("western_comic.cbz", "Galactic Guardians", 1, num_pages=6)
    print("All sample comic archives generated successfully!")
