"""Reusable playing card deck generator for Video Poker Android.

Implements the standardized 450x630 layout documented in docs/CARD_DECK_SPEC.md.
Default configuration regenerates the Modern Crest deck (app/src/main/assets/cards/)
using Bahnschrift (Weight=500, Width=78) and the extracted court/crest masters in
scripts/modern_crest_parts/, leaving back.png untouched.
"""

from __future__ import annotations

import argparse
import os
from dataclasses import dataclass
from typing import Dict, Optional, Tuple

from PIL import Image, ImageDraw, ImageFont


@dataclass(frozen=True)
class DeckStyleSpec:
    width: int = 450
    height: int = 630
    scale: int = 4
    corner_radius: int = 32
    border_px: int = 3
    border_color: Tuple[int, int, int, int] = (0, 0, 0, 255)
    black_color: Tuple[int, int, int, int] = (0, 0, 0, 255)
    red_color: Tuple[int, int, int, int] = (212, 0, 0, 255)
    white_color: Tuple[int, int, int, int] = (255, 255, 255, 255)

    # Corner rank typography
    font_path: str = r"C:\Windows\Fonts\bahnschrift.ttf"
    font_axes: Optional[Tuple[int, int]] = (500, 78)  # (Weight, Width)
    rank_font_size: int = 165
    rank_target_h: int = 110
    rank_ten_max_w: int = 92
    rank_ten_gap: int = 5
    rank_center_x: int = 72
    rank_top_y: int = 24
    rank_min_x: int = 24

    # Corner index pip
    index_pip_h: int = 112
    index_pip_w: int = 104
    index_pip_diamond_w: int = 96
    index_pip_gap: int = 16

    # Upper-right illustration bay (J, Q, K, A)
    art_bay_x: int = 140
    art_bay_y: int = 18
    art_bay_w: int = 292
    art_bay_h: int = 306

    # Lower primary pip
    primary_pip_h: int = 246
    primary_pip_w: int = 238
    primary_pip_diamond_w: int = 224
    primary_pip_cx: int = 206
    primary_pip_cy: int = 462


def cubic_bezier(
    p0: Tuple[float, float],
    p1: Tuple[float, float],
    p2: Tuple[float, float],
    p3: Tuple[float, float],
    steps: int = 36,
):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        mt = 1.0 - t
        x = (mt**3) * p0[0] + 3 * (mt**2) * t * p1[0] + 3 * mt * (t**2) * p2[0] + (t**3) * p3[0]
        y = (mt**3) * p0[1] + 3 * (mt**2) * t * p1[1] + 3 * mt * (t**2) * p2[1] + (t**3) * p3[1]
        pts.append((x, y))
    return pts


def draw_pip_ss(suit: str, box_w_px: int, box_h_px: int, color: Tuple[int, int, int, int], scale: int) -> Image.Image:
    """Draw a 4x supersampled vector suit pip into the exact requested pixel box."""
    sw, sh = box_w_px * scale, box_h_px * scale
    img = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def pt(x: float, y: float) -> Tuple[float, float]:
        return (x * sw, y * sh)

    if suit == "d":
        d.polygon([pt(0.50, 0.02), pt(0.96, 0.50), pt(0.50, 0.98), pt(0.04, 0.50)], fill=color)
    elif suit == "h":
        pts = (
            cubic_bezier(pt(0.50, 0.23), pt(0.45, 0.04), pt(0.18, 0.00), pt(0.06, 0.16))
            + cubic_bezier(pt(0.06, 0.16), pt(-0.02, 0.30), pt(0.06, 0.55), pt(0.50, 0.97))
            + cubic_bezier(pt(0.50, 0.97), pt(0.94, 0.55), pt(1.02, 0.30), pt(0.94, 0.16))
            + cubic_bezier(pt(0.94, 0.16), pt(0.82, 0.00), pt(0.55, 0.04), pt(0.50, 0.23))
        )
        d.polygon(pts, fill=color)
    elif suit == "s":
        pts = (
            cubic_bezier(pt(0.50, 0.02), pt(0.42, 0.18), pt(0.03, 0.42), pt(0.03, 0.65))
            + cubic_bezier(pt(0.03, 0.65), pt(0.03, 0.83), pt(0.24, 0.87), pt(0.43, 0.74))
            + [pt(0.45, 0.72), pt(0.41, 0.88), pt(0.28, 0.97), pt(0.72, 0.97), pt(0.59, 0.88), pt(0.55, 0.72)]
            + cubic_bezier(pt(0.57, 0.74), pt(0.76, 0.87), pt(0.97, 0.83), pt(0.97, 0.65))
            + cubic_bezier(pt(0.97, 0.65), pt(0.97, 0.42), pt(0.58, 0.18), pt(0.50, 0.02))
        )
        d.polygon(pts, fill=color)
    elif suit == "c":
        r_top, r_side = 0.245, 0.240

        def circle(cx: float, cy: float, r: float):
            d.ellipse(((cx - r) * sw, (cy - r) * sh, (cx + r) * sw, (cy + r) * sh), fill=color)

        circle(0.50, 0.26, r_top)
        circle(0.265, 0.59, r_side)
        circle(0.735, 0.59, r_side)
        d.polygon([pt(0.38, 0.44), pt(0.62, 0.44), pt(0.68, 0.64), pt(0.32, 0.64)], fill=color)
        d.polygon([pt(0.46, 0.60), pt(0.54, 0.60), pt(0.59, 0.87), pt(0.73, 0.97), pt(0.27, 0.97), pt(0.41, 0.87)], fill=color)
    else:
        raise ValueError(f"Unknown suit: {suit}")

    return img.resize((box_w_px, box_h_px), Image.Resampling.LANCZOS)


def load_rank_font(spec: DeckStyleSpec) -> ImageFont.FreeTypeFont:
    font = ImageFont.truetype(spec.font_path, spec.rank_font_size * spec.scale)
    if spec.font_axes is not None:
        font.set_variation_by_axes(list(spec.font_axes))
    return font


def render_rank_glyph(rank: str, color: Tuple[int, int, int, int], font: ImageFont.FreeTypeFont, spec: DeckStyleSpec) -> Image.Image:
    s = spec.scale
    target_h = spec.rank_target_h
    if rank.upper() == "T":
        c1 = Image.new("RGBA", (220 * s, 280 * s), (0, 0, 0, 0))
        ImageDraw.Draw(c1).text((30, 30), "1", font=font, fill=color)
        g1 = c1.crop(c1.getbbox())

        c0 = Image.new("RGBA", (260 * s, 280 * s), (0, 0, 0, 0))
        ImageDraw.Draw(c0).text((30, 30), "0", font=font, fill=color)
        g0 = c0.crop(c0.getbbox())

        gap = spec.rank_ten_gap * s
        h = max(g1.height, g0.height)
        combined = Image.new("RGBA", (g1.width + gap + g0.width, h), (0, 0, 0, 0))
        combined.paste(g1, (0, (h - g1.height) // 2), g1)
        combined.paste(g0, (g1.width + gap, (h - g0.height) // 2), g0)
        ratio = target_h / combined.height
        w_out = min(spec.rank_ten_max_w, int(round(combined.width * ratio)))
        return combined.resize((w_out, target_h), Image.Resampling.LANCZOS)

    c = Image.new("RGBA", (320 * s, 280 * s), (0, 0, 0, 0))
    ImageDraw.Draw(c).text((30, 30), rank.upper(), font=font, fill=color)
    g = c.crop(c.getbbox())
    ratio = target_h / g.height
    return g.resize((int(round(g.width * ratio)), target_h), Image.Resampling.LANCZOS)


def draw_card_frame(spec: DeckStyleSpec) -> Image.Image:
    sw, sh = spec.width * 2, spec.height * 2
    card = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    d = ImageDraw.Draw(card)
    if spec.border_px > 0:
        d.rounded_rectangle(
            (0, 0, sw - 1, sh - 1),
            radius=spec.corner_radius * 2,
            fill=spec.white_color,
            outline=spec.border_color,
            width=spec.border_px * 2,
        )
    else:
        d.rounded_rectangle(
            (0, 0, sw - 1, sh - 1),
            radius=spec.corner_radius * 2,
            fill=spec.white_color,
        )
    return card.resize((spec.width, spec.height), Image.Resampling.LANCZOS)


def load_artwork_sprites(parts_dir: str) -> Dict[str, Image.Image]:
    mapping = {
        "a": "A_crest.png",
        "j": "J_drone.png",
        "q": "Q_tablet.png",
        "k": "K_selfie.png",
    }
    sprites: Dict[str, Image.Image] = {}
    for rank_key, filename in mapping.items():
        path = os.path.join(parts_dir, filename)
        if os.path.isfile(path):
            sprites[rank_key] = Image.open(path).convert("RGBA")
    return sprites


def generate_deck(spec: DeckStyleSpec, parts_dir: str, out_dir: str) -> int:
    os.makedirs(out_dir, exist_ok=True)
    font = load_rank_font(spec)
    sprites = load_artwork_sprites(parts_dir)
    frame_template = draw_card_frame(spec)

    ranks = ["2", "3", "4", "5", "6", "7", "8", "9", "t", "j", "q", "k", "a"]
    suits = ["c", "d", "h", "s"]

    index_pips: Dict[str, Image.Image] = {}
    primary_pips: Dict[str, Image.Image] = {}
    for suit in suits:
        col = spec.red_color if suit in ("h", "d") else spec.black_color
        iw = spec.index_pip_diamond_w if suit == "d" else spec.index_pip_w
        pw = spec.primary_pip_diamond_w if suit == "d" else spec.primary_pip_w
        index_pips[suit] = draw_pip_ss(suit, iw, spec.index_pip_h, col, spec.scale)
        primary_pips[suit] = draw_pip_ss(suit, pw, spec.primary_pip_h, col, spec.scale)

    count = 0
    for suit in suits:
        col = spec.red_color if suit in ("h", "d") else spec.black_color
        for rank in ranks:
            card = frame_template.copy()

            art = sprites.get(rank)
            if art is not None:
                ax = spec.art_bay_x + (spec.art_bay_w - art.width) // 2
                ay = spec.art_bay_y + (spec.art_bay_h - art.height) // 2
                card.paste(art, (ax, ay), art)

            rg = render_rank_glyph(rank, col, font, spec)
            rx = max(spec.rank_min_x, spec.rank_center_x - rg.width // 2)
            ry = spec.rank_top_y
            card.paste(rg, (rx, ry), rg)

            ip = index_pips[suit]
            ix = spec.rank_center_x - ip.width // 2
            iy = ry + rg.height + spec.index_pip_gap
            card.paste(ip, (ix, iy), ip)

            pp = primary_pips[suit]
            px = spec.primary_pip_cx - pp.width // 2
            py = spec.primary_pip_cy - pp.height // 2
            card.paste(pp, (px, py), pp)

            card.save(os.path.join(out_dir, f"{rank}{suit}.png"), optimize=True)
            count += 1

    return count


def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description="Generate standardized 450x630 playing card deck faces.")
    parser.add_argument("--parts-dir", default=os.path.join(repo_root, "scripts", "modern_crest_parts"))
    parser.add_argument("--out-dir", default=os.path.join(repo_root, "app", "src", "main", "assets", "cards"))
    parser.add_argument("--weight", type=int, default=500, help="Bahnschrift Weight axis (default: 500)")
    parser.add_argument("--width-axis", type=int, default=78, help="Bahnschrift Width axis (default: 78)")
    parser.add_argument("--borderless", action="store_true", help="Omit outer black border stroke")
    args = parser.parse_args()

    spec = DeckStyleSpec(
        font_axes=(args.weight, args.width_axis),
        border_px=0 if args.borderless else 3,
    )
    generated = generate_deck(spec, args.parts_dir, args.out_dir)
    print(f"Generated {generated} cards in {args.out_dir} ({spec.width}x{spec.height}, Weight={args.weight})")


if __name__ == "__main__":
    main()
