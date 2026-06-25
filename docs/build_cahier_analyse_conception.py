from __future__ import annotations

from pathlib import Path
from textwrap import wrap

from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "Cahier_Analyse_Conception_Reconciliation.docx"
QA_DIR = ROOT / "qa"
QA_DIR.mkdir(parents=True, exist_ok=True)

BLUE = "2E74B5"
DARK_BLUE = "1F4D78"
NAVY = "0B2545"
MUTED = "5F6B7A"
LIGHT_BLUE = "E8EEF5"
LIGHT_GRAY = "F2F4F7"
CALLOUT = "F4F6F9"
WHITE = "FFFFFF"
BLACK = "111111"
GOLD = "A16C00"
RED = "9B1C1C"
GREEN = "1F5C3A"

PAGE_WIDTH_DXA = 9360
TABLE_INDENT_DXA = 120
CELL_TOP_BOTTOM = 80
CELL_START_END = 120


def font_file(bold: bool = False) -> str:
    candidates = [
        Path("C:/Windows/Fonts/calibrib.ttf" if bold else "C:/Windows/Fonts/calibri.ttf"),
        Path("C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return str(candidate)
    raise FileNotFoundError("Aucune police Calibri/Arial disponible")


def set_run_font(run, size: float | None = None, color: str = BLACK, bold: bool | None = None,
                 italic: bool | None = None, name: str = "Calibri"):
    run.font.name = name
    run._element.get_or_add_rPr().rFonts.set(qn("w:ascii"), name)
    run._element.get_or_add_rPr().rFonts.set(qn("w:hAnsi"), name)
    if size is not None:
        run.font.size = Pt(size)
    if color:
        run.font.color.rgb = RGBColor.from_string(color)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def set_cell_shading(cell, fill: str):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)
    shd.set(qn("w:val"), "clear")


def set_cell_margins(cell, top=CELL_TOP_BOTTOM, start=CELL_START_END,
                     bottom=CELL_TOP_BOTTOM, end=CELL_START_END):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for m, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{m}"))
        if node is None:
            node = OxmlElement(f"w:{m}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_table_borders(table, color="B7C3D0", size="4"):
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.find(qn("w:tblBorders"))
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        element = borders.find(qn(f"w:{edge}"))
        if element is None:
            element = OxmlElement(f"w:{edge}")
            borders.append(element)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), size)
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), color)


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def set_table_geometry(table, widths_dxa: list[int], indent_dxa=TABLE_INDENT_DXA):
    assert sum(widths_dxa) == PAGE_WIDTH_DXA, (widths_dxa, sum(widths_dxa))
    table.autofit = False
    table.alignment = WD_TABLE_ALIGNMENT.LEFT
    tbl_pr = table._tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    tbl_w.set(qn("w:w"), str(PAGE_WIDTH_DXA))
    tbl_w.set(qn("w:type"), "dxa")
    tbl_ind = tbl_pr.find(qn("w:tblInd"))
    if tbl_ind is None:
        tbl_ind = OxmlElement("w:tblInd")
        tbl_pr.append(tbl_ind)
    tbl_ind.set(qn("w:w"), str(indent_dxa))
    tbl_ind.set(qn("w:type"), "dxa")

    grid = table._tbl.tblGrid
    for child in list(grid):
        grid.remove(child)
    for width in widths_dxa:
        grid_col = OxmlElement("w:gridCol")
        grid_col.set(qn("w:w"), str(width))
        grid.append(grid_col)

    for row in table.rows:
        for idx, cell in enumerate(row.cells):
            width = widths_dxa[min(idx, len(widths_dxa) - 1)]
            tc_pr = cell._tc.get_or_add_tcPr()
            tc_w = tc_pr.find(qn("w:tcW"))
            if tc_w is None:
                tc_w = OxmlElement("w:tcW")
                tc_pr.append(tc_w)
            tc_w.set(qn("w:w"), str(width))
            tc_w.set(qn("w:type"), "dxa")
            cell.width = Inches(width / 1440)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_margins(cell)


def set_paragraph_text(cell, text: str, *, bold=False, color=BLACK, size=9.5,
                       align=WD_ALIGN_PARAGRAPH.LEFT):
    p = cell.paragraphs[0]
    p.alignment = align
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.10
    p.clear()
    r = p.add_run(str(text))
    set_run_font(r, size=size, color=color, bold=bold)


def add_table(doc, headers: list[str], rows: list[tuple], widths_dxa: list[int],
              font_size=9.2, first_col_bold=False, indent_dxa=TABLE_INDENT_DXA):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    set_table_geometry(table, widths_dxa, indent_dxa=indent_dxa)
    set_table_borders(table)
    header = table.rows[0]
    set_repeat_table_header(header)
    for i, value in enumerate(headers):
        set_cell_shading(header.cells[i], LIGHT_BLUE)
        set_paragraph_text(header.cells[i], value, bold=True, color=NAVY, size=9.2,
                           align=WD_ALIGN_PARAGRAPH.CENTER if len(value) < 18 else WD_ALIGN_PARAGRAPH.LEFT)
    for row_values in rows:
        row = table.add_row()
        for i, value in enumerate(row_values):
            set_paragraph_text(row.cells[i], "" if value is None else str(value),
                               bold=first_col_bold and i == 0, size=font_size,
                               align=WD_ALIGN_PARAGRAPH.CENTER if i == 0 and len(str(value)) < 15 else WD_ALIGN_PARAGRAPH.LEFT)
        set_table_geometry(table, widths_dxa, indent_dxa=indent_dxa)
    after = doc.add_paragraph()
    after.paragraph_format.space_before = Pt(0)
    after.paragraph_format.space_after = Pt(4)
    return table


def add_callout(doc, label: str, text: str, fill=CALLOUT, color=NAVY):
    table = doc.add_table(rows=1, cols=1)
    set_table_geometry(table, [PAGE_WIDTH_DXA])
    set_table_borders(table, color="D5DDE6", size="4")
    set_repeat_table_header(table.rows[0])
    cell = table.cell(0, 0)
    set_cell_shading(cell, fill)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.15
    r = p.add_run(label + "  ")
    set_run_font(r, size=10.2, color=color, bold=True)
    r = p.add_run(text)
    set_run_font(r, size=10.2, color=BLACK)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def add_numbering_definition(doc, num_fmt: str, level_text: str, bullet=False) -> int:
    numbering = doc.part.numbering_part.element
    abstract_ids = [int(e.get(qn("w:abstractNumId"))) for e in numbering.findall(qn("w:abstractNum"))]
    num_ids = [int(e.get(qn("w:numId"))) for e in numbering.findall(qn("w:num"))]
    abstract_id = max(abstract_ids, default=0) + 1
    num_id = max(num_ids, default=0) + 1

    abstract = OxmlElement("w:abstractNum")
    abstract.set(qn("w:abstractNumId"), str(abstract_id))
    multi = OxmlElement("w:multiLevelType")
    multi.set(qn("w:val"), "singleLevel")
    abstract.append(multi)
    lvl = OxmlElement("w:lvl")
    lvl.set(qn("w:ilvl"), "0")
    start = OxmlElement("w:start")
    start.set(qn("w:val"), "1")
    lvl.append(start)
    fmt = OxmlElement("w:numFmt")
    fmt.set(qn("w:val"), num_fmt)
    lvl.append(fmt)
    txt = OxmlElement("w:lvlText")
    txt.set(qn("w:val"), level_text)
    lvl.append(txt)
    jc = OxmlElement("w:lvlJc")
    jc.set(qn("w:val"), "left")
    lvl.append(jc)
    ppr = OxmlElement("w:pPr")
    tabs = OxmlElement("w:tabs")
    tab = OxmlElement("w:tab")
    tab.set(qn("w:val"), "num")
    tab.set(qn("w:pos"), "540")
    tabs.append(tab)
    ppr.append(tabs)
    ind = OxmlElement("w:ind")
    ind.set(qn("w:left"), "540")
    ind.set(qn("w:hanging"), "270")
    ppr.append(ind)
    spacing = OxmlElement("w:spacing")
    spacing.set(qn("w:after"), "80")
    spacing.set(qn("w:line"), "300")
    spacing.set(qn("w:lineRule"), "auto")
    ppr.append(spacing)
    lvl.append(ppr)
    if bullet:
        rpr = OxmlElement("w:rPr")
        fonts = OxmlElement("w:rFonts")
        fonts.set(qn("w:ascii"), "Calibri")
        fonts.set(qn("w:hAnsi"), "Calibri")
        rpr.append(fonts)
        lvl.append(rpr)
    abstract.append(lvl)
    numbering.append(abstract)

    num = OxmlElement("w:num")
    num.set(qn("w:numId"), str(num_id))
    abstract_ref = OxmlElement("w:abstractNumId")
    abstract_ref.set(qn("w:val"), str(abstract_id))
    num.append(abstract_ref)
    numbering.append(num)
    return num_id


def apply_numbering(paragraph, num_id: int):
    ppr = paragraph._p.get_or_add_pPr()
    num_pr = ppr.find(qn("w:numPr"))
    if num_pr is None:
        num_pr = OxmlElement("w:numPr")
        ppr.append(num_pr)
    ilvl = OxmlElement("w:ilvl")
    ilvl.set(qn("w:val"), "0")
    nid = OxmlElement("w:numId")
    nid.set(qn("w:val"), str(num_id))
    num_pr.append(ilvl)
    num_pr.append(nid)


def add_list(doc, items: list[str], num_id: int):
    for item in items:
        p = doc.add_paragraph()
        apply_numbering(p, num_id)
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.25
        r = p.add_run(item)
        set_run_font(r, size=11)


def add_body(doc, text: str, *, bold_lead: str | None = None):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.line_spacing = 1.25
    if bold_lead and text.startswith(bold_lead):
        r = p.add_run(bold_lead)
        set_run_font(r, size=11, bold=True, color=NAVY)
        r = p.add_run(text[len(bold_lead):])
        set_run_font(r, size=11)
    else:
        r = p.add_run(text)
        set_run_font(r, size=11)
    return p


def add_caption(doc, text: str):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(8)
    r = p.add_run(text)
    set_run_font(r, size=9, color=MUTED, italic=True)


def add_picture(doc, path: Path, width=6.2, alt_text="Schéma"):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    inline = run.add_picture(str(path), width=Inches(width))
    c_nv_pr = inline._inline.docPr
    c_nv_pr.set("descr", alt_text)
    p.paragraph_format.space_after = Pt(2)


def add_heading(doc, text: str, level: int):
    p = doc.add_paragraph(style=f"Heading {level}")
    r = p.add_run(text)
    set_run_font(r, size={1: 16, 2: 13, 3: 12}[level],
                 color=BLUE if level in (1, 2) else DARK_BLUE, bold=True)
    return p


def add_use_case_spec(doc, *, uc_id: str, title: str, actor: str, objective: str,
                      preconditions: str, trigger: str, steps: list[str],
                      alternatives: list[str], postconditions: str, api: str,
                      decimal_num_id: int, bullet_num_id: int):
    add_heading(doc, f"{uc_id} — {title}", 3)
    add_body(doc, f"Acteur principal : {actor}", bold_lead="Acteur principal :")
    add_body(doc, f"Objectif : {objective}", bold_lead="Objectif :")
    add_body(doc, f"Préconditions : {preconditions}", bold_lead="Préconditions :")
    add_body(doc, f"Déclencheur : {trigger}", bold_lead="Déclencheur :")
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run("Scénario nominal")
    set_run_font(r, size=10.5, bold=True, color=NAVY)
    add_list(doc, steps, decimal_num_id)
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run("Alternatives et erreurs")
    set_run_font(r, size=10.5, bold=True, color=NAVY)
    add_list(doc, alternatives, bullet_num_id)
    add_body(doc, f"Postconditions : {postconditions}", bold_lead="Postconditions :")
    add_body(doc, f"API principales : {api}", bold_lead="API principales :")


def start_section(doc, title: str, *, page_break=True):
    if page_break:
        doc.add_page_break()
    return add_heading(doc, title, 1)


def add_page_field(paragraph, field_name: str):
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), field_name)
    r = OxmlElement("w:r")
    rpr = OxmlElement("w:rPr")
    color = OxmlElement("w:color")
    color.set(qn("w:val"), MUTED)
    rpr.append(color)
    r.append(rpr)
    t = OxmlElement("w:t")
    t.text = "1"
    r.append(t)
    fld.append(r)
    paragraph._p.append(fld)


def configure_styles(doc):
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)
    section.header_distance = Inches(0.492)
    section.footer_distance = Inches(0.492)
    section.different_first_page_header_footer = True

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Calibri"
    normal._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    normal._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    normal.font.size = Pt(11)
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.25

    for level, size, color, before, after in (
        (1, 16, BLUE, 18, 10),
        (2, 13, BLUE, 14, 7),
        (3, 12, DARK_BLUE, 10, 5),
    ):
        style = styles[f"Heading {level}"]
        style.font.name = "Calibri"
        style._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
        style._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor.from_string(color)
        style.paragraph_format.space_before = Pt(before)
        style.paragraph_format.space_after = Pt(after)
        style.paragraph_format.keep_with_next = True

    if "Document Title" not in [s.name for s in styles]:
        title = styles.add_style("Document Title", WD_STYLE_TYPE.PARAGRAPH)
    else:
        title = styles["Document Title"]
    title.font.name = "Calibri"
    title._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    title._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    title.font.size = Pt(30)
    title.font.bold = True
    title.font.color.rgb = RGBColor.from_string(NAVY)
    title.paragraph_format.space_before = Pt(0)
    title.paragraph_format.space_after = Pt(8)

    header = section.header
    hp = header.paragraphs[0]
    hp.alignment = WD_ALIGN_PARAGRAPH.LEFT
    hp.paragraph_format.space_after = Pt(0)
    r = hp.add_run("CAHIER D’ANALYSE ET DE CONCEPTION  |  RECONCILLIATION-SERVICE")
    set_run_font(r, size=8.5, color=MUTED, bold=True)

    footer = section.footer
    fp = footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    fp.paragraph_format.space_before = Pt(0)
    fp.paragraph_format.space_after = Pt(0)
    r = fp.add_run("État du code au 22 juin 2026  •  Page ")
    set_run_font(r, size=8, color=MUTED)
    add_page_field(fp, "PAGE")
    r = fp.add_run(" / ")
    set_run_font(r, size=8, color=MUTED)
    add_page_field(fp, "NUMPAGES")


def rounded_box(draw, xy, title, body, fill, outline, title_font, body_font, text_color="#0B2545"):
    x1, y1, x2, y2 = xy
    draw.rounded_rectangle(xy, radius=18, fill=fill, outline=outline, width=3)
    draw.text(((x1 + x2) / 2, y1 + 18), title, anchor="ma", font=title_font, fill=text_color)
    lines = []
    for part in body.split("\n"):
        lines.extend(wrap(part, width=max(18, int((x2 - x1) / 13))))
    y = y1 + 58
    for line in lines[:5]:
        draw.text(((x1 + x2) / 2, y), line, anchor="ma", font=body_font, fill="#333333")
        y += 24


def arrow(draw, start, end, color="#60758A", width=5):
    draw.line([start, end], fill=color, width=width)
    x2, y2 = end
    x1, y1 = start
    import math
    angle = math.atan2(y2 - y1, x2 - x1)
    length = 16
    wing = 0.55
    p1 = (x2 - length * math.cos(angle - wing), y2 - length * math.sin(angle - wing))
    p2 = (x2 - length * math.cos(angle + wing), y2 - length * math.sin(angle + wing))
    draw.polygon([end, p1, p2], fill=color)


def make_functional_diagram(path: Path):
    img = Image.new("RGB", (1800, 1030), "white")
    draw = ImageDraw.Draw(img)
    title_font = ImageFont.truetype(font_file(True), 30)
    body_font = ImageFont.truetype(font_file(False), 23)
    main_font = ImageFont.truetype(font_file(True), 42)
    draw.text((900, 42), "Chaîne fonctionnelle de la plateforme", anchor="ma", font=main_font, fill="#0B2545")
    sources = [
        ("BANQUE", "Carthago\nMOOV / ORANGE"),
        ("MOOV", "Exports Wallet"),
        ("ORANGE", "Exports Mobile Money"),
        ("AMPLITUDE", "Écritures comptables"),
    ]
    xs = [50, 485, 920, 1355]
    for x, (t, b) in zip(xs, sources):
        rounded_box(draw, (x, 120, x + 395, 280), t, b, "#F4F6F9", "#9FB4C8", title_font, body_font)
        arrow(draw, (x + 198, 280), (x + 198, 365))
    rounded_box(draw, (170, 370, 1630, 535), "INGESTION ET QUALITÉ",
                "Validation des en-têtes • parsing CSV/XLS/XLSX • normalisation • dédoublonnage • traçabilité",
                "#E8EEF5", "#2E74B5", title_font, body_font)
    arrow(draw, (900, 535), (900, 620))
    rounded_box(draw, (455, 625, 1345, 790), "COEUR DE TRAITEMENT",
                "Rapprochement Banque ↔ opérateur • classification • contrôle AMPLITUDE • calcul des positions",
                "#EAF4EE", "#3F7C5A", title_font, body_font)
    arrow(draw, (900, 790), (900, 855))
    outputs = [
        ("ANALYSE", "Résultats et anomalies"),
        ("PILOTAGE", "Dashboard et reporting"),
        ("FINANCE", "Comptabilisation et compensation"),
    ]
    xs2 = [110, 655, 1200]
    for x, (t, b) in zip(xs2, outputs):
        rounded_box(draw, (x, 860, x + 490, 1000), t, b, "#FFF7E6", "#B58A2C", title_font, body_font)
    img.save(path)


def make_architecture_diagram(path: Path):
    img = Image.new("RGB", (1800, 1120), "white")
    draw = ImageDraw.Draw(img)
    title_font = ImageFont.truetype(font_file(True), 29)
    body_font = ImageFont.truetype(font_file(False), 22)
    main_font = ImageFont.truetype(font_file(True), 42)
    draw.text((900, 40), "Architecture logique constatée", anchor="ma", font=main_font, fill="#0B2545")
    layers = [
        (105, 280, "EXPOSITION REST", "Contrôleurs /api • validation HTTP • pagination • exports", "#E8EEF5", "#2E74B5"),
        (345, 555, "SERVICES APPLICATIFS", "Import • rapprochement • dashboard • reporting • comptabilisation • compensation • rétention", "#F4F6F9", "#60758A"),
        (620, 810, "COMPOSANTS MÉTIER", "Parseurs CSV/XLS/XLSX • normalisation des statuts • stratégies MOOV/ORANGE • classification", "#EAF4EE", "#3F7C5A"),
        (875, 1060, "PERSISTANCE ET INFRASTRUCTURE", "Spring Data JPA • PostgreSQL/Flyway • stockage local des fichiers • mail/Jasper/PDF/POI", "#FFF7E6", "#B58A2C"),
    ]
    for y1, y2, t, b, fill, outline in layers:
        rounded_box(draw, (170, y1, 1630, y2), t, b, fill, outline, title_font, body_font)
    for y in (280, 555, 810):
        arrow(draw, (900, y), (900, y + 60))
    img.save(path)


def make_data_diagram(path: Path):
    img = Image.new("RGB", (1800, 1050), "white")
    draw = ImageDraw.Draw(img)
    title_font = ImageFont.truetype(font_file(True), 27)
    body_font = ImageFont.truetype(font_file(False), 20)
    main_font = ImageFont.truetype(font_file(True), 40)
    draw.text((900, 38), "Modèle de données simplifié", anchor="ma", font=main_font, fill="#0B2545")
    boxes = {
        "import": (60, 120, 500, 360, "FILE_IMPORT", "source • opérateur • date métier\nchecksum • statut • compteurs"),
        "run": (620, 120, 1180, 360, "RECONCILIATION_RUN", "opérateur • période • imports utilisés\nstatut • synthèse"),
        "bank": (40, 510, 400, 760, "BANK_TRANSACTION", "transactionId • statut • montant\ncompte • téléphone • référence"),
        "moov": (445, 510, 805, 760, "MOOV_TRANSACTION", "receiptNo • statut • type\nMSISDN • montant • dates"),
        "orange": (995, 510, 1355, 760, "ORANGE_TRANSACTION", "omTransactionId • compte alias\ntéléphone • statut • montant"),
        "amp": (1400, 510, 1760, 760, "AMPLITUDE_TRANSACTION", "référence • compte • téléphone\nmontant • date • direction"),
        "result": (620, 845, 1180, 1015, "RECONCILIATION_RESULT", "clé • type • montants • raison\nliens Banque / opérateur"),
    }
    for key, (x1, y1, x2, y2, t, b) in boxes.items():
        fill = "#E8EEF5" if key in ("import", "run") else "#F4F6F9"
        rounded_box(draw, (x1, y1, x2, y2), t, b, fill, "#60758A", title_font, body_font)
    # FILE_IMPORT possède les lignes de chacune des quatre sources.
    draw.line([(280, 360), (280, 435), (220, 435), (1580, 435)], fill="#60758A", width=5)
    for x in (220, 625, 1175, 1580):
        draw.line([(x, 435), (x, 475)], fill="#60758A", width=5)
        arrow(draw, (x, 475), (x, 500))
    # Le run possède ses résultats ; les IDs Banque/opérateur y sont référencés logiquement.
    arrow(draw, (900, 360), (900, 835))
    arrow(draw, (400, 705), (610, 920))
    arrow(draw, (805, 705), (810, 835))
    arrow(draw, (995, 705), (990, 835))
    img.save(path)


def draw_actor(draw, x: int, y: int, label: str, font):
    draw.ellipse((x - 22, y - 72, x + 22, y - 28), outline="#0B2545", width=4)
    draw.line((x, y - 28, x, y + 34), fill="#0B2545", width=4)
    draw.line((x - 38, y - 2, x + 38, y - 2), fill="#0B2545", width=4)
    draw.line((x, y + 34, x - 34, y + 82), fill="#0B2545", width=4)
    draw.line((x, y + 34, x + 34, y + 82), fill="#0B2545", width=4)
    draw.text((x, y + 98), label, anchor="ma", font=font, fill="#0B2545")


def draw_use_case(draw, center, size, text, font):
    cx, cy = center
    w, h = size
    draw.ellipse((cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2),
                 fill="#F4F6F9", outline="#2E74B5", width=3)
    lines = wrap(text, width=26)
    y = cy - (len(lines) - 1) * 14
    for line in lines:
        draw.text((cx, y), line, anchor="mm", font=font, fill="#0B2545")
        y += 28


def make_use_case_diagram(path: Path):
    img = Image.new("RGB", (1800, 1240), "white")
    draw = ImageDraw.Draw(img)
    title_font = ImageFont.truetype(font_file(True), 40)
    uc_font = ImageFont.truetype(font_file(False), 20)
    actor_font = ImageFont.truetype(font_file(True), 22)
    draw.text((900, 38), "Diagramme global des cas d’utilisation", anchor="ma", font=title_font, fill="#0B2545")
    boundary = (310, 100, 1490, 1170)
    draw.rounded_rectangle(boundary, radius=20, fill="#FBFCFE", outline="#60758A", width=4)
    draw.text((900, 125), "Système de réconciliation", anchor="ma", font=actor_font, fill="#5F6B7A")

    cases = {
        "import": (560, 245, "Importer les fichiers"),
        "consult": (900, 245, "Consulter imports et transactions"),
        "run": (1240, 245, "Lancer une réconciliation"),
        "analyze": (560, 470, "Analyser les résultats et anomalies"),
        "dashboard": (900, 470, "Consulter le tableau de bord"),
        "report": (1240, 470, "Produire un reporting"),
        "accounting": (560, 695, "Contrôler la comptabilisation AMPLITUDE"),
        "compensation": (900, 695, "Analyser la compensation"),
        "delete": (1240, 695, "Prévisualiser et supprimer des imports"),
        "retention": (560, 920, "Exécuter la rétention"),
        "admin": (900, 920, "Gérer utilisateurs, profils et rôles"),
        "operate": (1240, 920, "Configurer et exploiter la plateforme"),
    }
    actor_positions = {
        "Agent de rapprochement": (135, 285),
        "Contrôle comptable": (135, 735),
        "Superviseur": (1660, 285),
        "Administrateur": (1660, 700),
        "Exploitant": (1660, 1030),
    }
    links = {
        "Agent de rapprochement": ["import", "consult", "run", "analyze", "delete"],
        "Contrôle comptable": ["accounting", "compensation", "report"],
        "Superviseur": ["analyze", "dashboard", "report", "compensation"],
        "Administrateur": ["admin"],
        "Exploitant": ["delete", "retention", "operate"],
    }
    # Associations first so the use-case ellipses remain readable above the lines.
    for actor, keys in links.items():
        ax, ay = actor_positions[actor]
        start_x = 205 if ax < 900 else 1595
        for key in keys:
            cx, cy, _ = cases[key]
            end_x = cx - 155 if ax < 900 else cx + 155
            draw.line((start_x, ay - 5, end_x, cy), fill="#A3AFBD", width=3)
    for _, (cx, cy, text) in cases.items():
        draw_use_case(draw, (cx, cy), (310, 118), text, uc_font)
    for label, (x, y) in actor_positions.items():
        draw_actor(draw, x, y, label, actor_font)
    img.save(path)


def draw_class_box(draw, xy, name: str, attributes: list[str], operations: list[str],
                   title_font, body_font, fill="#F4F6F9"):
    x1, y1, x2, y2 = xy
    draw.rounded_rectangle(xy, radius=12, fill=fill, outline="#60758A", width=3)
    header_y = y1 + 54
    draw.line((x1, header_y, x2, header_y), fill="#60758A", width=2)
    draw.text(((x1 + x2) // 2, y1 + 26), name, anchor="mm", font=title_font, fill="#0B2545")
    y = header_y + 18
    for attr in attributes:
        draw.text((x1 + 14, y), attr, anchor="la", font=body_font, fill="#333333")
        y += 24
    if operations:
        op_line = min(y + 4, y2 - 70)
        draw.line((x1, op_line, x2, op_line), fill="#60758A", width=2)
        y = op_line + 16
        for op in operations:
            draw.text((x1 + 14, y), op, anchor="la", font=body_font, fill="#333333")
            y += 24


def make_class_diagram(path: Path):
    img = Image.new("RGB", (1800, 1260), "white")
    draw = ImageDraw.Draw(img)
    main_font = ImageFont.truetype(font_file(True), 40)
    title_font = ImageFont.truetype(font_file(True), 20)
    body_font = ImageFont.truetype(font_file(False), 16)
    label_font = ImageFont.truetype(font_file(True), 18)
    draw.text((900, 38), "Diagramme de classes du domaine", anchor="ma", font=main_font, fill="#0B2545")

    classes = {
        "file": ((620, 105, 1180, 330), "FileImport",
                 ["id: Long", "sourceType: SourceType", "operatorScope: OperatorType", "businessDate: LocalDate",
                  "checksum: String", "importStatus: ImportStatus", "validRows / invalidRows: Integer"], []),
        "bank": ((40, 445, 410, 720), "BankTransaction",
                 ["id: Long", "transactionId: String", "allocationStatusNormalized", "amount: BigDecimal",
                  "operationReference: String", "operationNature: String", "transactionDate: LocalDateTime"], []),
        "moov": ((465, 445, 835, 720), "MoovTransaction",
                 ["id: Long", "receiptNo: String", "transactionStatusNormalized", "transactionType: String",
                  "msisdn: String", "amount: BigDecimal", "completionTime: LocalDateTime"], []),
        "orange": ((965, 445, 1335, 720), "OrangeTransaction",
                   ["id: Long", "omTransactionId: String", "aliasBankAccountNumber: String",
                    "transactionStatusNormalized", "senderMobileNumber: String", "amount: BigDecimal"], []),
        "amp": ((1390, 445, 1760, 720), "AmplitudeTransaction",
                ["id: Long", "operationReference: String", "accountNumber: String", "phoneNumber: String",
                 "amount: BigDecimal", "operationDate: LocalDateTime", "direction: String"], []),
        "run": ((260, 890, 770, 1160), "ReconciliationRun",
                ["id: Long", "label: String", "operator: OperatorType", "businessDateFrom / To",
                 "bankImportIds: String", "operatorImportIds: String", "status: RunStatus"], []),
        "result": ((1030, 890, 1540, 1160), "ReconciliationResult",
                   ["id: Long", "businessDate: LocalDate", "transactionKey: String", "resultType: ResultType",
                    "bankTransactionId: Long", "operatorTransactionId: Long", "amountDifference: BigDecimal"], []),
    }
    # Relations are drawn first.
    draw.line((900, 330, 900, 382), fill="#60758A", width=4)
    draw.line((220, 382, 1575, 382), fill="#60758A", width=4)
    for x in (220, 650, 1150, 1575):
        arrow(draw, (x, 382), (x, 435), width=4)
    arrow(draw, (515, 890), (515, 730), width=4)
    arrow(draw, (770, 1025), (1020, 1025), width=4)
    arrow(draw, (410, 660), (1018, 965), width=3)
    arrow(draw, (835, 660), (1050, 900), width=3)
    arrow(draw, (965, 660), (1200, 880), width=3)
    draw.text((925, 350), "1", anchor="la", font=label_font, fill="#5F6B7A")
    draw.text((235, 400), "0..*", anchor="la", font=label_font, fill="#5F6B7A")
    draw.text((785, 990), "1        0..*", anchor="la", font=label_font, fill="#5F6B7A")
    for _, (xy, name, attrs, ops) in classes.items():
        draw_class_box(draw, xy, name, attrs, ops, title_font, body_font,
                       fill="#E8EEF5" if name in ("FileImport", "ReconciliationRun") else "#F4F6F9")
    img.save(path)


def make_sequence_diagram(path: Path, title: str, participants: list[str], steps: list[tuple[int, int, str, bool]]):
    img = Image.new("RGB", (1800, 1160), "white")
    draw = ImageDraw.Draw(img)
    main_font = ImageFont.truetype(font_file(True), 38)
    title_font = ImageFont.truetype(font_file(True), 18)
    body_font = ImageFont.truetype(font_file(False), 17)
    draw.text((900, 38), title, anchor="ma", font=main_font, fill="#0B2545")
    count = len(participants)
    xs = [140 + i * (1520 // max(1, count - 1)) for i in range(count)]
    for x, participant in zip(xs, participants):
        draw.rounded_rectangle((x - 115, 92, x + 115, 160), radius=10, fill="#E8EEF5", outline="#2E74B5", width=3)
        lines = wrap(participant, width=18)
        yy = 120 - (len(lines) - 1) * 10
        for line in lines:
            draw.text((x, yy), line, anchor="mm", font=title_font, fill="#0B2545")
            yy += 21
        y = 160
        while y < 1100:
            draw.line((x, y, x, min(y + 16, 1100)), fill="#98A7B6", width=2)
            y += 28
    y = 230
    for source, target, label, response in steps:
        sx, tx = xs[source], xs[target]
        color = "#7A8795" if response else "#2E74B5"
        if response:
            # dashed response line
            left, right = sorted((sx, tx))
            xx = left
            while xx < right:
                draw.line((xx, y, min(xx + 14, right), y), fill=color, width=3)
                xx += 24
            arrow(draw, (sx, y), (tx, y), color=color, width=1)
        else:
            arrow(draw, (sx, y), (tx, y), color=color, width=4)
        midpoint = (sx + tx) // 2
        draw.rectangle((midpoint - 235, y - 32, midpoint + 235, y - 6), fill="white")
        draw.text((midpoint, y - 19), label, anchor="mm", font=body_font, fill="#333333")
        y += 86
    img.save(path)


def make_activity_import(path: Path):
    img = Image.new("RGB", (1800, 1250), "white")
    draw = ImageDraw.Draw(img)
    main_font = ImageFont.truetype(font_file(True), 40)
    title_font = ImageFont.truetype(font_file(True), 22)
    body_font = ImageFont.truetype(font_file(False), 19)
    draw.text((900, 38), "Diagramme d’activité — Import d’un fichier", anchor="ma", font=main_font, fill="#0B2545")

    def node(cx, cy, text, fill="#E8EEF5", w=620, h=82):
        draw.rounded_rectangle((cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2),
                               radius=18, fill=fill, outline="#2E74B5", width=3)
        draw.text((cx, cy), text, anchor="mm", font=title_font, fill="#0B2545")

    def decision(cx, cy, text):
        pts = [(cx, cy - 62), (cx + 165, cy), (cx, cy + 62), (cx - 165, cy)]
        draw.polygon(pts, fill="#FFF7E6", outline="#B58A2C")
        draw.line(pts + [pts[0]], fill="#B58A2C", width=3)
        draw.text((cx, cy), text, anchor="mm", font=body_font, fill="#0B2545")

    draw.ellipse((875, 82, 925, 132), fill="#0B2545")
    node(900, 190, "Valider source, opérateur, date et extension")
    decision(900, 315, "Fichier déjà importé ?")
    node(1340, 315, "Refuser avec l’import existant", fill="#FFF1F1", w=470)
    node(900, 450, "Stocker le fichier et créer FileImport = PENDING")
    node(900, 570, "Parser et contrôler les en-têtes")
    decision(900, 695, "Structure valide ?")
    node(1340, 695, "Marquer FAILED et conserver l’erreur", fill="#FFF1F1", w=520)
    node(900, 830, "Pour chaque ligne : valider, normaliser et dédoublonner", w=760)
    decision(900, 965, "Résultat du traitement")
    node(420, 1090, "SUCCESS", fill="#EAF4EE", w=320)
    node(900, 1090, "PARTIAL_SUCCESS", fill="#FFF7E6", w=420)
    node(1380, 1090, "FAILED", fill="#FFF1F1", w=320)
    draw.ellipse((875, 1180, 925, 1230), outline="#0B2545", width=4)
    draw.ellipse((886, 1191, 914, 1219), fill="#0B2545")
    for a, b in [((900, 132), (900, 145)), ((900, 231), (900, 247)), ((900, 377), (900, 405)),
                 ((900, 491), (900, 525)), ((900, 611), (900, 633)), ((900, 757), (900, 785)),
                 ((900, 871), (900, 903))]:
        arrow(draw, a, b)
    arrow(draw, (1065, 315), (1100, 315))
    draw.text((1080, 290), "Oui", anchor="mm", font=body_font, fill="#5F6B7A")
    draw.text((925, 392), "Non", anchor="la", font=body_font, fill="#5F6B7A")
    arrow(draw, (1065, 695), (1080, 695))
    draw.text((1075, 670), "Non", anchor="mm", font=body_font, fill="#5F6B7A")
    draw.text((925, 775), "Oui", anchor="la", font=body_font, fill="#5F6B7A")
    for x, label in ((420, "0 invalide"), (900, "Valides + invalides"), (1380, "0 valide")):
        arrow(draw, (900, 1027), (x, 1040))
        draw.text(((900 + x) // 2, 1025), label, anchor="ms", font=body_font, fill="#5F6B7A")
        arrow(draw, (x, 1131), (900, 1170))
    img.save(path)


def make_activity_reconciliation(path: Path):
    img = Image.new("RGB", (1800, 1200), "white")
    draw = ImageDraw.Draw(img)
    main_font = ImageFont.truetype(font_file(True), 40)
    title_font = ImageFont.truetype(font_file(True), 21)
    body_font = ImageFont.truetype(font_file(False), 18)
    draw.text((900, 38), "Diagramme d’activité — Réconciliation", anchor="ma", font=main_font, fill="#0B2545")

    def node(cx, cy, text, fill="#E8EEF5", w=700, h=78):
        draw.rounded_rectangle((cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2),
                               radius=16, fill=fill, outline="#2E74B5", width=3)
        draw.text((cx, cy), text, anchor="mm", font=title_font, fill="#0B2545")

    def decision(cx, cy, text):
        pts = [(cx, cy - 58), (cx + 170, cy), (cx, cy + 58), (cx - 170, cy)]
        draw.polygon(pts, fill="#FFF7E6", outline="#B58A2C")
        draw.line(pts + [pts[0]], fill="#B58A2C", width=3)
        draw.text((cx, cy), text, anchor="mm", font=body_font, fill="#0B2545")

    draw.ellipse((875, 78, 925, 128), fill="#0B2545")
    node(900, 180, "Valider opérateur, date ou période et libellé")
    node(900, 285, "Charger les imports Banque et opérateur")
    node(900, 390, "Créer ReconciliationRun = RUNNING")
    decision(900, 510, "Canal sélectionné")
    node(490, 630, "Stratégie MOOV : transactionId ↔ receiptNo", w=660)
    node(1310, 630, "Stratégie ORANGE : transactionId ↔ omTransactionId", w=690)
    node(900, 750, "Construire l’union des clés et détecter les doublons", w=820)
    node(900, 855, "Classifier chaque paire et persister ReconciliationResult", w=820)
    node(900, 960, "Calculer la synthèse et les KPI financiers")
    node(900, 1065, "Marquer le run COMPLETED", fill="#EAF4EE")
    draw.ellipse((875, 1135, 925, 1185), outline="#0B2545", width=4)
    draw.ellipse((886, 1146, 914, 1174), fill="#0B2545")
    for a, b in [((900, 128), (900, 140)), ((900, 221), (900, 246)), ((900, 326), (900, 351)),
                 ((900, 431), (900, 452)), ((490, 671), (780, 712)), ((1310, 671), (1020, 712)),
                 ((900, 791), (900, 816)), ((900, 896), (900, 921)), ((900, 1001), (900, 1026)),
                 ((900, 1106), (900, 1125))]:
        arrow(draw, a, b)
    arrow(draw, (730, 510), (490, 585))
    arrow(draw, (1070, 510), (1310, 585))
    draw.text((620, 545), "MOOV", anchor="mm", font=body_font, fill="#5F6B7A")
    draw.text((1180, 545), "ORANGE", anchor="mm", font=body_font, fill="#5F6B7A")
    img.save(path)


def make_state_diagram(path: Path):
    img = Image.new("RGB", (1800, 980), "white")
    draw = ImageDraw.Draw(img)
    main_font = ImageFont.truetype(font_file(True), 40)
    title_font = ImageFont.truetype(font_file(True), 26)
    body_font = ImageFont.truetype(font_file(False), 19)
    draw.text((900, 38), "Diagrammes d’états", anchor="ma", font=main_font, fill="#0B2545")

    def state(cx, cy, text, fill="#E8EEF5", w=310):
        draw.rounded_rectangle((cx - w // 2, cy - 48, cx + w // 2, cy + 48), radius=24,
                               fill=fill, outline="#2E74B5", width=3)
        draw.text((cx, cy), text, anchor="mm", font=title_font, fill="#0B2545")

    draw.text((450, 110), "Cycle de vie FileImport", anchor="ma", font=title_font, fill="#5F6B7A")
    draw.ellipse((110, 190, 158, 238), fill="#0B2545")
    state(350, 214, "PENDING")
    state(350, 420, "SUCCESS", fill="#EAF4EE")
    state(700, 420, "PARTIAL_SUCCESS", fill="#FFF7E6", w=360)
    state(350, 630, "FAILED", fill="#FFF1F1")
    state(600, 820, "SUPPRIMÉ", fill="#F2F4F7")
    arrow(draw, (158, 214), (185, 214))
    arrow(draw, (350, 262), (350, 365))
    arrow(draw, (505, 214), (650, 365))
    arrow(draw, (350, 262), (350, 575))
    arrow(draw, (350, 468), (560, 785))
    arrow(draw, (700, 468), (630, 765))
    arrow(draw, (350, 678), (560, 785))
    draw.text((380, 315), "toutes les lignes valides", anchor="la", font=body_font, fill="#5F6B7A")
    draw.text((570, 270), "valides + invalides", anchor="la", font=body_font, fill="#5F6B7A")
    draw.text((175, 510), "erreur de structure\nou aucune ligne valide", anchor="la", font=body_font, fill="#5F6B7A")

    draw.line((900, 100, 900, 900), fill="#D5DDE6", width=3)
    draw.text((1350, 110), "Cycle de vie ReconciliationRun", anchor="ma", font=title_font, fill="#5F6B7A")
    draw.ellipse((990, 190, 1038, 238), fill="#0B2545")
    state(1225, 214, "RUNNING")
    state(1470, 430, "COMPLETED", fill="#EAF4EE", w=340)
    state(1120, 650, "FAILED (cible)", fill="#FFF1F1", w=350)
    state(1470, 820, "ARCHIVÉ / PURGÉ", fill="#F2F4F7", w=390)
    arrow(draw, (1038, 214), (1070, 214))
    arrow(draw, (1380, 250), (1440, 380))
    arrow(draw, (1225, 262), (1140, 595))
    arrow(draw, (1470, 478), (1470, 765))
    arrow(draw, (1250, 685), (1380, 785))
    draw.text((1440, 315), "traitement réussi", anchor="mm", font=body_font, fill="#5F6B7A")
    draw.text((1090, 420), "exception", anchor="mm", font=body_font, fill="#5F6B7A")
    img.save(path)


def make_deployment_diagram(path: Path):
    img = Image.new("RGB", (1800, 1120), "white")
    draw = ImageDraw.Draw(img)
    main_font = ImageFont.truetype(font_file(True), 40)
    title_font = ImageFont.truetype(font_file(True), 25)
    body_font = ImageFont.truetype(font_file(False), 20)
    draw.text((900, 38), "Diagramme de déploiement cible", anchor="ma", font=main_font, fill="#0B2545")

    rounded_box(draw, (70, 180, 470, 430), "« client »", "Navigateur / frontend\nHTTPS vers /api", "#F4F6F9", "#60758A", title_font, body_font)
    rounded_box(draw, (650, 130, 1230, 500), "« conteneur applicatif »",
                "Java 21 • Spring Boot\nJWT • REST • batch\nPort applicatif aligné",
                "#E8EEF5", "#2E74B5", title_font, body_font)
    rounded_box(draw, (1380, 180, 1730, 430), "« services mail »", "SMTP / IMAP", "#FFF7E6", "#B58A2C", title_font, body_font)
    rounded_box(draw, (170, 720, 620, 1010), "« volume fichiers »",
                "Uploads CSV/XLS/XLSX\nExports temporaires\nSauvegarde et rétention",
                "#F4F6F9", "#60758A", title_font, body_font)
    rounded_box(draw, (720, 690, 1270, 1030), "« base PostgreSQL 16 »",
                "Schéma Flyway V1–V28\nTransactions • runs • résultats\nArchives et backoffice",
                "#EAF4EE", "#3F7C5A", title_font, body_font)
    rounded_box(draw, (1410, 720, 1710, 990), "« supervision »",
                "Actuator\nLogs • métriques\nAlertes",
                "#FFF7E6", "#B58A2C", title_font, body_font)
    arrow(draw, (470, 305), (640, 305))
    draw.text((555, 280), "HTTPS / JWT", anchor="mm", font=body_font, fill="#5F6B7A")
    arrow(draw, (1230, 305), (1370, 305))
    draw.text((1300, 280), "SMTP/IMAP", anchor="mm", font=body_font, fill="#5F6B7A")
    arrow(draw, (790, 500), (570, 710))
    draw.text((650, 600), "filesystem", anchor="mm", font=body_font, fill="#5F6B7A")
    arrow(draw, (980, 500), (990, 680))
    draw.text((1030, 590), "JDBC", anchor="la", font=body_font, fill="#5F6B7A")
    arrow(draw, (1180, 500), (1430, 710))
    draw.text((1340, 600), "health/logs", anchor="mm", font=body_font, fill="#5F6B7A")
    img.save(path)


def cover(doc):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(24)
    p.paragraph_format.space_after = Pt(72)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("RAPPORT TECHNIQUE")
    set_run_font(r, size=11, color=GOLD, bold=True)

    p = doc.add_paragraph(style="Document Title")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Cahier d’analyse\net de conception")
    set_run_font(r, size=30, color=NAVY, bold=True)
    p.paragraph_format.space_after = Pt(12)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(32)
    r = p.add_run("Plateforme de réconciliation Banque – Mobile Money")
    set_run_font(r, size=15, color=DARK_BLUE, bold=True)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(78)
    r = p.add_run("Projet : reconcilliation-service")
    set_run_font(r, size=12, color=MUTED)

    table = doc.add_table(rows=3, cols=2)
    set_table_geometry(table, [2700, 6660])
    set_table_borders(table, color="D7E0E8", size="4")
    set_repeat_table_header(table.rows[0])
    metadata = [
        ("Version", "1.1 — enrichissement UML"),
        ("État de référence", "Code source analysé au 22 juin 2026"),
        ("Périmètre", "Backend Spring Boot, règles métier, données, API, sécurité et exploitation"),
    ]
    for row, values in zip(table.rows, metadata):
        set_cell_shading(row.cells[0], LIGHT_BLUE)
        set_paragraph_text(row.cells[0], values[0], bold=True, color=NAVY, size=10)
        set_paragraph_text(row.cells[1], values[1], size=10)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(36)
    r = p.add_run("Document établi à partir des contrôleurs, services, entités, migrations Flyway, configurations et tests du dépôt.")
    set_run_font(r, size=9.5, color=MUTED, italic=True)


def build_document():
    functional_diagram = QA_DIR / "architecture_fonctionnelle.png"
    architecture_diagram = QA_DIR / "architecture_logique.png"
    data_diagram = QA_DIR / "modele_donnees.png"
    use_case_diagram = QA_DIR / "diagramme_cas_utilisation.png"
    class_diagram = QA_DIR / "diagramme_classes.png"
    activity_import_diagram = QA_DIR / "activite_import.png"
    activity_reconciliation_diagram = QA_DIR / "activite_reconciliation.png"
    sequence_import_diagram = QA_DIR / "sequence_import.png"
    sequence_reconciliation_diagram = QA_DIR / "sequence_reconciliation.png"
    sequence_deletion_diagram = QA_DIR / "sequence_suppression.png"
    state_diagram = QA_DIR / "diagrammes_etats.png"
    deployment_diagram = QA_DIR / "diagramme_deploiement.png"
    make_functional_diagram(functional_diagram)
    make_architecture_diagram(architecture_diagram)
    make_data_diagram(data_diagram)
    make_use_case_diagram(use_case_diagram)
    make_class_diagram(class_diagram)
    make_activity_import(activity_import_diagram)
    make_activity_reconciliation(activity_reconciliation_diagram)
    make_sequence_diagram(
        sequence_import_diagram,
        "Diagramme de séquence — Import",
        ["Utilisateur", "ImportController", "FileImportService", "Parser", "Normalizer", "Repositories / Storage"],
        [
            (0, 1, "POST fichier + source + date", False),
            (1, 2, "importFile(...) ", False),
            (2, 5, "checksum, stockage, FileImport PENDING", False),
            (2, 3, "parse(fichier, source)", False),
            (3, 2, "lignes normalisées / erreur", True),
            (2, 4, "normaliser les statuts", False),
            (4, 2, "statuts internes", True),
            (2, 5, "sauver transactions et compteurs", False),
            (5, 2, "import persisté", True),
            (2, 0, "SUCCESS / PARTIAL_SUCCESS / FAILED", True),
        ],
    )
    make_sequence_diagram(
        sequence_reconciliation_diagram,
        "Diagramme de séquence — Réconciliation",
        ["Utilisateur", "ReconciliationController", "ReconciliationService", "Strategy", "Classification", "ResultRepository"],
        [
            (0, 1, "POST /api/reconciliations/run", False),
            (1, 2, "run(request)", False),
            (2, 5, "créer run RUNNING", False),
            (2, 3, "reconcile(run, banque, opérateur)", False),
            (3, 4, "classify(paire)", False),
            (4, 3, "ResultType", True),
            (3, 5, "sauver chaque résultat", False),
            (3, 2, "traitement terminé", True),
            (2, 5, "synthèse + COMPLETED", False),
            (2, 0, "run et KPI", True),
        ],
    )
    make_sequence_diagram(
        sequence_deletion_diagram,
        "Diagramme de séquence — Suppression sécurisée",
        ["Utilisateur", "ImportController", "FileImportService", "Run / Result Repositories", "Transaction Repository", "FileStorage"],
        [
            (0, 1, "GET preview-delete", False),
            (1, 2, "construire le plan", False),
            (2, 3, "identifier runs et résultats", False),
            (3, 2, "IDs et compteurs impactés", True),
            (2, 0, "aperçu + confirmation requise", True),
            (0, 1, "DELETE confirmCascade=true", False),
            (1, 2, "exécuter le même plan", False),
            (2, 3, "supprimer résultats puis runs", False),
            (2, 4, "supprimer transactions et import", False),
            (2, 5, "supprimer le fichier physique", False),
        ],
    )
    make_state_diagram(state_diagram)
    make_deployment_diagram(deployment_diagram)

    doc = Document()
    configure_styles(doc)
    bullet_num_id = add_numbering_definition(doc, "bullet", "•", bullet=True)
    decimal_num_id = add_numbering_definition(doc, "decimal", "%1.")

    props = doc.core_properties
    props.title = "Cahier d’analyse et de conception – Reconcilliation Service"
    props.subject = "Analyse fonctionnelle et conception technique de la plateforme de réconciliation"
    props.author = "OpenAI Codex"
    props.keywords = "réconciliation, MOOV, Orange, AMPLITUDE, Spring Boot, analyse, conception"

    cover(doc)
    doc.add_page_break()

    add_heading(doc, "Synthèse exécutive", 1)
    add_callout(
        doc,
        "Conclusion générale",
        "Le projet constitue un backend métier cohérent couvrant l’ensemble de la chaîne de contrôle : ingestion des fichiers, rapprochement Banque–opérateur, analyse des anomalies, contrôle AMPLITUDE, compensation, reporting et rétention. L’architecture est modulaire et extensible par stratégie opérateur. Les priorités de consolidation portent sur l’harmonisation des règles MOOV/Orange, la validation des runs, la sécurité fine des API, la fidélité de l’archivage et l’alignement de la configuration de déploiement.",
    )
    add_body(doc, "Le dépôt contient 160 fichiers Java, 28 migrations Flyway et 13 fichiers de test. La commande mvn test exécutée le 22 juin 2026 termine avec succès : 28 tests exécutés, aucune erreur et aucun échec. Les classes d’intégration suffixées IT sont présentes, mais ne sont pas exécutées par le cycle Surefire par défaut.")
    add_body(doc, "Le présent cahier associe l’analyse fonctionnelle aux vues UML nécessaires à la conception : contexte, cas d’utilisation, activités, séquences, classes, états, composants, données et déploiement.")
    add_table(
        doc,
        ["Axe", "Constat principal", "Appréciation"],
        [
            ("Couverture fonctionnelle", "Import, rapprochement, analyse, reporting, comptabilisation, compensation, nettoyage et administration.", "Large"),
            ("Conception", "Contrôleurs, services, stratégies opérateur, repositories et migrations séparés.", "Structurée"),
            ("Traçabilité", "Imports, lignes sources, raw payload, runs, résultats et raisons codifiées.", "Bonne"),
            ("Robustesse", "Dédoublonnage, normalisation, pagination, rétention par lot et tests unitaires.", "À renforcer sur les cas limites"),
            ("Sécurité", "JWT en production, BCrypt et profils ; autorisations surtout globales, peu granulaires.", "À durcir"),
            ("Exploitation", "PostgreSQL/Flyway, stockage local et Docker ; incohérences de ports et secrets à externaliser.", "À corriger avant industrialisation"),
        ],
        [1800, 5660, 1900],
        font_size=9.1,
        first_col_bold=True,
    )

    add_heading(doc, "Sommaire", 1)
    sections = [
        ("1", "Cadre, objectifs et méthode"),
        ("2", "Contexte métier, acteurs et périmètre"),
        ("3", "Cartographie fonctionnelle et diagramme de cas d’utilisation"),
        ("4", "Analyse détaillée des fonctionnalités"),
        ("5", "Règles de gestion et décisions métier"),
        ("6", "Spécification des cas d’utilisation, activités et séquences"),
        ("7", "Conception UML et architecture technique"),
        ("8", "Modèle de données"),
        ("9", "Catalogue des API REST"),
        ("10", "Sécurité, configuration et exploitation"),
        ("11", "Tests, recette et qualité"),
        ("12", "Écarts, risques et recommandations"),
        ("13", "Feuille de route et traçabilité"),
    ]
    add_table(doc, ["N°", "Section"], sections, [850, 8510], font_size=10, first_col_bold=True)

    add_heading(doc, "Liste des figures", 1)
    add_table(doc, ["Figure", "Intitulé", "Section"], [
        ("1", "Contexte et chaîne fonctionnelle", "3.1"),
        ("2", "Diagramme global des cas d’utilisation", "3.3"),
        ("3", "Diagramme d’activité de l’import", "6.5"),
        ("4", "Diagramme d’activité de la réconciliation", "6.5"),
        ("5", "Diagramme de séquence de l’import", "6.6"),
        ("6", "Diagramme de séquence de la réconciliation", "6.6"),
        ("7", "Diagramme de séquence de la suppression sécurisée", "6.6"),
        ("8", "Diagramme de composants", "7.1"),
        ("9", "Diagramme de classes du domaine", "7.5"),
        ("10", "Diagrammes d’états", "7.6"),
        ("11", "Diagramme de déploiement", "7.7"),
        ("12", "Modèle de données simplifié", "8"),
    ], [1000, 6460, 1900], font_size=9.2, first_col_bold=True)

    start_section(doc, "1. Cadre, objectifs et méthode")
    add_heading(doc, "1.1 Objet du cahier", 2)
    add_body(doc, "Ce cahier formalise l’analyse fonctionnelle et la conception technique du service de réconciliation. Il décrit le comportement effectivement observé dans le code afin de servir de référentiel commun aux équipes métier, développement, recette, sécurité et exploitation.")
    add_heading(doc, "1.2 Objectifs", 2)
    add_list(doc, [
        "Définir le périmètre fonctionnel couvert et les acteurs concernés.",
        "Décrire les flux d’import, de rapprochement, de contrôle comptable et de compensation.",
        "Formaliser les règles de normalisation, de dédoublonnage, de classification et de calcul des KPI.",
        "Présenter l’architecture, les données persistées, les API et les exigences non fonctionnelles.",
        "Identifier les écarts entre l’intention documentaire et l’implémentation, puis proposer une trajectoire d’amélioration.",
    ], bullet_num_id)
    add_heading(doc, "1.3 Méthode d’analyse", 2)
    add_body(doc, "L’analyse a été réalisée par lecture croisée des artefacts suivants : contrôleurs REST, interfaces et implémentations de services, parseurs, stratégies de rapprochement, entités et repositories JPA, migrations Flyway V1 à V28, configurations Spring Security, propriétés d’application, Docker, README et tests. Lorsque la documentation et le code divergent, le comportement du code est retenu comme référence, et l’écart est signalé.")
    add_callout(doc, "Convention", "Les expressions « constaté » et « actuel » décrivent l’implémentation au 22 juin 2026. Les éléments marqués « cible » ou « recommandé » sont des choix de conception proposés, non encore garantis par le code.")

    start_section(doc, "2. Contexte métier, acteurs et périmètre")
    add_heading(doc, "2.1 Problématique métier", 2)
    add_body(doc, "Les opérations Banque vers Wallet et Wallet vers Banque sont enregistrées par plusieurs systèmes : la Banque/Carthago, MOOV Money, Orange Money et le système comptable AMPLITUDE. Des écarts de présence, de statut, de montant ou de comptabilisation peuvent apparaître. La plateforme centralise les fichiers, rapproche les lignes sur une clé transactionnelle, classe les écarts et fournit des éléments de décision financière.")
    add_heading(doc, "2.2 Finalités attendues", 2)
    add_list(doc, [
        "Sécuriser le contrôle quotidien des flux Mobile Money.",
        "Identifier rapidement les débits à tort, crédits sans débit, absences, doublons et écarts de montant.",
        "Vérifier la présence des opérations Banque dans AMPLITUDE et quantifier le montant à risque.",
        "Consolider les positions Banque/opérateur pour la compensation.",
        "Produire des KPI, exports et historiques auditables.",
    ], bullet_num_id)
    add_heading(doc, "2.3 Acteurs", 2)
    add_table(doc, ["Acteur", "Responsabilités fonctionnelles", "Accès attendu"], [
        ("Agent de rapprochement", "Importer les fichiers, lancer les runs, analyser les anomalies, exporter les résultats.", "Lecture/écriture métier"),
        ("Contrôle comptable", "Contrôler AMPLITUDE, suivre les non-comptabilisés et les montants à risque.", "Comptabilisation et exports"),
        ("Responsable compensation", "Consulter les positions journalières, hebdomadaires et mensuelles ; traiter les écarts.", "Compensation et détail"),
        ("Superviseur / management", "Consulter tableaux de bord, reporting et tendances.", "Lecture consolidée"),
        ("Administrateur", "Gérer utilisateurs, profils, rôles, catégories, produits et paramètres.", "Administration"),
        ("Exploitant technique", "Configurer la base, le stockage, la rétention, le mail, les profils et le déploiement.", "Exploitation"),
    ], [1800, 5350, 2210], font_size=9.1, first_col_bold=True)
    add_callout(doc, "Point de conception", "Le code dispose des autorités BA_ADMIN et BA_CONNECT, mais n’applique pas d’autorisations métier fines au niveau des méthodes. La matrice des droits par acteur doit donc être validée et implémentée explicitement.", fill="FFF7E6", color=GOLD)
    add_heading(doc, "2.4 Périmètre", 2)
    add_table(doc, ["Inclus", "Hors périmètre ou non démontré"], [
        ("Backend REST, imports, données, rapprochement MOOV/Orange, AMPLITUDE, dashboard, reporting, compensation, rétention, administration.", "Écrans frontend, orchestration externe, traitement temps réel, workflow de correction/validation d’anomalie, rapprochement multidevise."),
    ], [4680, 4680], font_size=9.5)

    add_heading(doc, "2.5 Exigences fonctionnelles", 2)
    add_table(doc, ["ID", "Exigence", "Priorité", "Cas associé"], [
        ("RF-01", "Importer des fichiers Banque avec une portée MOOV ou Orange.", "Haute", "UC-01"),
        ("RF-02", "Importer les fichiers MOOV et Orange et contrôler leur structure.", "Haute", "UC-02"),
        ("RF-03", "Importer les écritures AMPLITUDE pour le contrôle comptable.", "Haute", "UC-03"),
        ("RF-04", "Tracer les lignes valides, invalides, dupliquées et les erreurs d’import.", "Haute", "UC-01 à UC-03"),
        ("RF-05", "Prévisualiser l’impact d’une suppression avant toute cascade.", "Haute", "UC-04"),
        ("RF-06", "Exiger une confirmation lorsqu’un import est utilisé par un run.", "Haute", "UC-04"),
        ("RF-07", "Exécuter une réconciliation par opérateur et période.", "Haute", "UC-05"),
        ("RF-08", "Classifier absences, statuts, écarts, échecs et doublons.", "Haute", "UC-05"),
        ("RF-09", "Filtrer, consulter et exporter les résultats détaillés.", "Haute", "UC-06"),
        ("RF-10", "Présenter les KPI, montants, tendances et indicateurs qualité.", "Moyenne", "UC-07"),
        ("RF-11", "Produire des rapports jour, semaine et mois en Excel/PDF.", "Moyenne", "UC-08"),
        ("RF-12", "Contrôler la comptabilisation et quantifier le montant à risque.", "Haute", "UC-09"),
        ("RF-13", "Calculer les positions et écarts de compensation.", "Haute", "UC-10"),
        ("RF-14", "Archiver ou purger les anciennes réconciliations par lots.", "Moyenne", "UC-11"),
        ("RF-15", "Gérer utilisateurs, profils, rôles et paramètres legacy.", "Moyenne", "UC-12"),
    ], [850, 5680, 1200, 1630], font_size=8.4, first_col_bold=True)

    start_section(doc, "3. Cartographie fonctionnelle et cas d’utilisation")
    add_heading(doc, "3.1 Diagramme de contexte fonctionnel", 2)
    add_picture(doc, functional_diagram, alt_text="Chaîne fonctionnelle de la plateforme de réconciliation")
    add_caption(doc, "Figure 1 — De l’ingestion des sources à l’analyse et à la décision financière.")
    add_heading(doc, "3.2 Modules", 2)
    add_table(doc, ["Module", "Fonction principale", "Objets clés"], [
        ("Import et qualité", "Acquérir, valider, normaliser, dédoublonner et tracer les fichiers.", "FileImport, transactions source"),
        ("Transactions", "Consulter les lignes persistées par import ou par identifiants.", "Bank/Moov/OrangeTransaction"),
        ("Réconciliation", "Créer un run, rapprocher les clés et classifier chaque situation.", "ReconciliationRun, Result"),
        ("Dashboard", "Présenter volumes, distribution, montants, timeline, anomalies et qualité.", "KPI et filtres"),
        ("Reporting", "Produire synthèses jour/semaine/mois et exports Excel/PDF.", "ReportingSummary"),
        ("Comptabilisation", "Vérifier les opérations Banque dans AMPLITUDE et calculer le risque.", "AccountingCheck/KPI"),
        ("Compensation", "Comparer les succès Banque/opérateur et fournir une décision.", "CompensationDaily/Period"),
        ("Rétention/nettoyage", "Archiver/purger l’historique et supprimer des imports avec impacts.", "Archives, bilans de suppression"),
        ("Administration", "Gérer identités, rôles, profils et catalogue legacy.", "BaUser, BaProfil, BaRole"),
    ], [1900, 4760, 2700], font_size=8.9, first_col_bold=True)
    add_heading(doc, "3.3 Diagramme de cas d’utilisation", 2)
    add_picture(doc, use_case_diagram, alt_text="Diagramme UML des acteurs et cas d’utilisation")
    add_caption(doc, "Figure 2 — Acteurs métier et services attendus de la plateforme.")
    add_heading(doc, "3.4 Relations acteurs–cas d’utilisation", 2)
    add_table(doc, ["Acteur", "Cas principaux", "Responsabilité"], [
        ("Agent de rapprochement", "UC-01, UC-02, UC-04, UC-05, UC-06", "Alimenter les données et traiter les anomalies."),
        ("Contrôle comptable", "UC-03, UC-08, UC-09, UC-10", "Vérifier AMPLITUDE et les positions financières."),
        ("Superviseur", "UC-06, UC-07, UC-08, UC-10", "Piloter les résultats, tendances et risques."),
        ("Administrateur", "UC-12", "Gérer identités, profils et rôles."),
        ("Exploitant", "UC-04, UC-11 et configuration", "Assurer la conservation, la disponibilité et le nettoyage."),
    ], [2100, 3150, 4110], font_size=9.0, first_col_bold=True)

    start_section(doc, "4. Analyse détaillée des fonctionnalités")
    add_heading(doc, "4.1 Import et ingestion des fichiers", 2)
    add_body(doc, "Le service accepte les formats CSV, XLS et XLSX. Il choisit le parseur selon l’extension, calcule un checksum SHA-256, stocke le fichier localement, crée une métadonnée d’import en statut PENDING, parse les lignes, persiste les transactions valides et termine en SUCCESS, PARTIAL_SUCCESS ou FAILED.")
    add_heading(doc, "Entrées et validations", 3)
    add_table(doc, ["Source", "Paramètres", "Contrôles structurants"], [
        ("BANQUE", "Fichier, opérateur MOOV/ORANGE, date métier", "Opérateur obligatoire ; clé, statut, montant et date requis."),
        ("MOOV", "Fichier, date métier", "Receipt No et statut requis ; receipt valide si longueur ≥ 11."),
        ("ORANGE", "Fichier, date métier", "ID OM, compte alias, date, montant et statut requis ; CSV forcé au séparateur « ; »."),
        ("AMPLITUDE", "Fichier, date métier", "Libellé requis ; les lignes non transactionnelles sont ignorées ; téléphone attendu."),
    ], [1400, 2950, 5010], font_size=9.0, first_col_bold=True)
    add_heading(doc, "Comportements", 3)
    add_list(doc, [
        "Recherche de la ligne d’en-tête dans les 30 premières lignes Excel ; suppression des lignes vides.",
        "Conservation du numéro de ligne source et du contenu brut sous forme textuelle pour diagnostic.",
        "Normalisation des montants, dates, comptes, téléphones, références et statuts.",
        "Dédoublonnage intra-fichier et vis-à-vis de la base sur la clé transactionnelle.",
        "Blocage d’un même fichier par source, date métier, portée opérateur et checksum ; récupération automatique d’une métadonnée orpheline sans transaction.",
        "Conservation d’un maximum de 50 avertissements détaillés pour les reçus MOOV invalides ou dupliqués.",
    ], bullet_num_id)
    add_heading(doc, "Suppression et prévisualisation", 3)
    add_body(doc, "Les imports peuvent être supprimés par dernier import, source/date, ou totalité d’une source. La suppression calcule les runs utilisant les identifiants d’import, supprime les résultats et runs impactés, supprime les transactions, la métadonnée et le fichier physique. Une API de prévisualisation expose les volumes avant action.")

    add_heading(doc, "4.2 Consultation des transactions", 2)
    add_body(doc, "Les transactions Banque, MOOV et Orange sont consultables par import avec pagination. Des endpoints GET et POST permettent aussi de charger des lignes par liste d’identifiants, notamment pour enrichir des vues de détail côté client.")

    add_heading(doc, "4.3 Exécution de la réconciliation", 2)
    add_body(doc, "Un run est défini par un libellé, un opérateur et une date ou plage de dates. L’opérateur par défaut est MOOV si le champ est absent. Le service sélectionne les imports Banque portant la même portée opérateur, crée le run RUNNING, charge les transactions, puis délègue au composant de stratégie MOOV ou Orange.")
    add_list(doc, [
        "MOOV rapproche BankTransaction.transactionId avec MoovTransaction.receiptNo.",
        "Orange rapproche BankTransaction.transactionId avec OrangeTransaction.omTransactionId.",
        "L’union des clés Banque et opérateur est parcourue ; chaque clé produit un ou plusieurs résultats.",
        "Les doublons Banque sont prioritaires sur les doublons opérateur.",
        "Le résultat conserve les identifiants, statuts bruts, montants, écart, raison et date métier résolue.",
        "Le run est finalisé COMPLETED et reçoit une synthèse textuelle.",
    ], bullet_num_id)
    add_heading(doc, "Consultation et export", 3)
    add_body(doc, "Les runs sont filtrables par opérateur, jour, plage ou préréglage (7 jours, 30 jours, 3 mois). Les résultats sont paginés et filtrables par type, référence d’opération, téléphone, compte et clé. Le CSV courant est minimal (clé et type) ; l’export XLSX contient les champs détaillés.")

    add_heading(doc, "4.4 Tableau de bord", 2)
    add_body(doc, "Le tableau de bord exige un canal et accepte une date métier ou une plage, avec filtres facultatifs runId et importId. Il produit la synthèse, la distribution des catégories, les montants, une timeline horaire sur 24 heures, les plus grosses anomalies et la qualité des imports.")
    add_table(doc, ["Indicateur", "Définition actuelle"], [
        ("Matching rate", "MATCH_OK rapporté au nombre de lignes Banque du périmètre financier."),
        ("Success rate", "MATCH_OK rapporté au nombre total de résultats financièrement pertinents."),
        ("Anomaly rate", "Résultats pertinents hors MATCH_OK rapportés au total pertinent."),
        ("Data quality", "Volumes valides/invalides, taux de parsing et doublons issus des résultats."),
        ("Timeline", "Comptage des événements Banque et opérateur par heure, avec anomalies par clé."),
    ], [2500, 6860], font_size=9.2, first_col_bold=True)

    add_heading(doc, "4.5 Reporting périodique", 2)
    add_body(doc, "Le reporting construit une fenêtre DAY, WEEK (lundi à dimanche) ou MONTH (premier au dernier jour). Il enrichit les résultats par les dates réelles des transactions, calcule les KPI, la distribution, la ventilation journalière et un détail de transactions. Le JSON est limité à 300 détails, le PDF à 80 données chargées puis 24 lignes affichées, et Excel exporte tous les détails.")
    add_list(doc, [
        "Excel : feuilles Synthèse, Distribution, Journalier et Transactions.",
        "PDF : KPI principaux, synthèse exécutive automatique et top détails.",
        "Indicateurs étendus : succès opérateur, succès Banque/Carthago, succès opérateur sans Carthago, hors périmètre, moyenne et pic journalier.",
    ], bullet_num_id)

    add_heading(doc, "4.6 Comptabilisation AMPLITUDE", 2)
    add_body(doc, "Le contrôle retient les transactions Banque normalisées SUCCESS_BANK sur la période et, si demandé, sur l’opérateur. Il cherche d’abord une transaction AMPLITUDE portant la même référence normalisée (au moins 10 chiffres), puis applique un fallback sur date ±1 jour, montant identique et compte identique lorsque les deux comptes sont disponibles.")
    add_list(doc, [
        "Une ligne trouvée est COMPTABILISE ; une ligne attendue mais absente est NON_COMPTABILISE.",
        "Un « paiement généré » absent d’AMPLITUDE est exclu du périmètre et ne contribue pas au risque.",
        "Les KPI couvrent volumes/montants comptabilisés, non comptabilisés, taux, montant à risque, écarts net/absolu et nombre d’écarts.",
        "Les exports KPI sont disponibles en CSV et PDF ; AMPLITUDE peut être nettoyé par jour, plage, semaine ou totalité.",
    ], bullet_num_id)

    add_heading(doc, "4.7 Compensation", 2)
    add_body(doc, "La compensation agrège, pour chaque jour et opérateur, le nombre et le montant des transactions normalisées en succès côté Banque et côté opérateur. La différence est montant Banque moins montant opérateur. Une différence nulle donne OK_COMPENSATION ; toute autre valeur donne A_VERIFIER.")
    add_body(doc, "Les vues disponibles sont journalière, hebdomadaire par plage ou semaine de référence, mensuelle, et détail paginé des écarts issus des résultats de réconciliation hors MATCH_OK et hors OPERATEUR_NON_ABOUTI_SANS_BANQUE.")

    add_heading(doc, "4.8 Rétention et nettoyage", 2)
    add_body(doc, "La rétention est désactivée par défaut. Lorsqu’elle est activée ou déclenchée manuellement, elle calcule une date limite (180 jours par défaut), traite par lots de 5 000 et applique ARCHIVE_AND_PURGE ou PURGE_ONLY. Les résultats sont traités avant les runs afin de respecter les dépendances.")
    add_body(doc, "Le cron par défaut est 02 h 30 chaque jour. L’API permet de fournir une date limite, un nombre de jours ou un mode. Les archives sont stockées dans reconciliation_result_archive et reconciliation_run_archive.")

    add_heading(doc, "4.9 Administration et services transverses", 2)
    add_list(doc, [
        "Authentification JWT, activation, changement et réinitialisation de mot de passe.",
        "CRUD des utilisateurs, rôles et profils ; relation profil–rôles et utilisateur–profil.",
        "CRUD des catégories et produits, journalisation métier et rapport Jasper legacy.",
        "Stockage de documents, envoi d’e-mails asynchrone avec template Thymeleaf et tentative d’archivage IMAP.",
        "Audit JPA des créations/modifications sur les entités backoffice héritées.",
    ], bullet_num_id)

    start_section(doc, "5. Règles de gestion et décisions métier")
    add_heading(doc, "5.1 Colonnes obligatoires par source", 2)
    add_table(doc, ["Source", "Groupes d’en-têtes acceptés"], [
        ("BANQUE", "Transaction ID ; statut allocation ; montant ; date transaction (alias français/anglais et champs techniques)."),
        ("MOOV", "Receipt No ; Transaction Status."),
        ("ORANGE", "OM_TRANSACTION_ID ; ALIAS_BANKACCOUNTNUMBER ; TRANSACTION_DATE_TIME ; montant ; statut."),
        ("AMPLITUDE", "Libellé."),
    ], [1600, 7760], font_size=9.3, first_col_bold=True)

    add_heading(doc, "5.2 Normalisation des statuts", 2)
    add_table(doc, ["Source", "Succès", "Échec", "Inconnu"], [
        ("BANQUE", "alloué, paiement généré, allocated, deallocated, paymentissued", "non alloué, paiement rejeté, échec allocation, paymentrejected, allocationfailed", "Toute autre valeur"),
        ("MOOV", "completed", "cancelled", "Toute autre valeur"),
        ("ORANGE", "TS", "TF", "Toute autre valeur"),
    ], [1350, 3100, 3100, 1810], font_size=8.8, first_col_bold=True)

    add_heading(doc, "5.3 Matrice de classification", 2)
    add_table(doc, ["Situation", "MOOV", "ORANGE", "Commentaire"], [
        ("Opérateur succès, Banque absente", "ABSENT_COTE_BANQUE", "ABSENT_COTE_BANQUE", "Anomalie financière."),
        ("Opérateur échec, Banque absente", "OPERATEUR_NON_ABOUTI_SANS_BANQUE", "Identique", "Exclu des KPI financiers."),
        ("Banque seule, succès", "ABSENT_COTE_MOOV", "DEBIT_A_TORT", "Asymétrie constatée."),
        ("Banque seule, échec", "ABSENT_COTE_MOOV", "ABSENT_COTE_ORANGE", "Asymétrie constatée."),
        ("Statut inconnu", "STATUT_INCONNU", "STATUT_INCONNU", "Prioritaire sur le cas AMPLITUDE."),
        ("Paiement généré + référence AMPLITUDE", "MATCH_OK", "MATCH_OK", "Après contrôle des statuts inconnus."),
        ("Deux succès", "MATCH_OK", "MATCH_OK", "MOOV vérifie d’abord la tolérance montant."),
        ("Banque succès / opérateur échec", "DEBIT_A_TORT", "DEBIT_A_TORT", "Débit sans aboutissement opérateur."),
        ("Banque échec / opérateur succès", "CREDIT_SANS_DEBIT", "CREDIT_SANS_DEBIT", "Crédit opérateur sans débit Banque."),
        ("Deux échecs", "ECHEC_DES_DEUX_COTES", "ECHEC_DES_DEUX_COTES", "MOOV peut devenir MONTANT_DIFFERENT si montants différents."),
        ("Écart montant > tolérance", "MONTANT_DIFFERENT", "Non contrôlé dans la classification", "Écart majeur à harmoniser."),
        ("Plusieurs lignes Banque", "DOUBLON_BANQUE", "DOUBLON_BANQUE", "Un résultat par ligne Banque."),
        ("Plusieurs lignes opérateur", "DOUBLON_MOOV", "DOUBLON_MOOV", "Libellé historique réutilisé pour Orange."),
    ], [2260, 1910, 1910, 3280], font_size=8.1, first_col_bold=True)
    add_callout(doc, "Règle financière", "La tolérance de montant est configurable via app.reconciliation.amount-tolerance. Dans l’implémentation actuelle, elle est appliquée à MOOV mais pas à Orange.", fill="FFF1F1", color=RED)

    add_heading(doc, "5.4 Date métier d’un résultat", 2)
    add_numbered = [
        "Date réelle de transaction Banque si disponible.",
        "Sinon date opérateur : completionTime puis initiationTime pour MOOV ; transactionDateTime pour Orange.",
        "Sinon date métier de l’import Banque.",
        "Sinon date métier de l’import opérateur.",
        "Sinon date de début du run.",
    ]
    add_list(doc, add_numbered, decimal_num_id)

    add_heading(doc, "5.5 Dédoublonnage et idempotence", 2)
    add_body(doc, "L’import refuse une clé transactionnelle déjà traitée dans le fichier ou déjà présente en base. Un index unique protège aussi le quadruplet source/date/portée opérateur/checksum. Les runs de réconciliation, en revanche, ne possèdent pas de clé d’idempotence : un même périmètre peut être relancé et produire un nouvel historique.")

    add_heading(doc, "5.6 Calculs principaux", 2)
    add_table(doc, ["Calcul", "Formule"], [
        ("Écart de résultat", "bankAmount − operatorAmount"),
        ("Taux", "compteur × 100 / total, arrondi à 2 décimales"),
        ("Écart compensation", "somme succès Banque − somme succès opérateur"),
        ("Montant à risque comptable", "somme des transactions allouées attendues mais absentes d’AMPLITUDE"),
        ("Fenêtre semaine", "lundi au dimanche autour de la date de référence"),
        ("Fenêtre mois", "premier au dernier jour du mois de référence"),
    ], [2650, 6710], font_size=9.2, first_col_bold=True)

    start_section(doc, "6. Spécification des cas d’utilisation, activités et séquences")
    add_heading(doc, "6.1 Catalogue des cas d’utilisation", 2)
    add_table(doc, ["ID", "Cas", "Acteur", "Résultat attendu"], [
        ("UC-01", "Importer un fichier Banque", "Agent", "Import tracé, lignes valides persistées, erreurs comptées."),
        ("UC-02", "Importer un fichier opérateur", "Agent", "MOOV ou Orange disponible pour rapprochement."),
        ("UC-03", "Importer AMPLITUDE", "Comptable", "Écritures disponibles pour contrôle comptable."),
        ("UC-04", "Prévisualiser/supprimer un import", "Superviseur", "Impacts connus puis suppression cohérente."),
        ("UC-05", "Lancer une réconciliation", "Agent", "Run terminé avec résultats et synthèse."),
        ("UC-06", "Analyser une anomalie", "Agent", "Détail filtré et exportable."),
        ("UC-07", "Consulter le dashboard", "Superviseur", "KPI, distribution, timeline et qualité."),
        ("UC-08", "Produire un rapport", "Superviseur", "Synthèse DAY/WEEK/MONTH et export."),
        ("UC-09", "Contrôler AMPLITUDE", "Comptable", "Statut comptable et risque calculés."),
        ("UC-10", "Analyser la compensation", "Responsable", "Position et décision OK/A_VERIFIER."),
        ("UC-11", "Exécuter la rétention", "Exploitant", "Données archivées/purgées avec bilan."),
        ("UC-12", "Administrer les accès", "Administrateur", "Identités et profils à jour."),
    ], [850, 2700, 1660, 4150], font_size=8.8, first_col_bold=True)

    add_heading(doc, "6.2 Description détaillée des cas d’utilisation", 2)
    add_use_case_spec(
        doc, uc_id="UC-01", title="Importer un fichier Banque", actor="Agent de rapprochement",
        objective="Charger les transactions Carthago destinées à un canal opérateur.",
        preconditions="Utilisateur authentifié ; fichier CSV/XLS/XLSX ; opérateur et date métier connus.",
        trigger="L’utilisateur sélectionne le fichier Banque et lance l’import.",
        steps=["Transmettre le fichier, l’opérateur et la date métier.", "Calculer le checksum et vérifier l’absence de doublon.",
               "Stocker le fichier et créer l’import PENDING.", "Parser, normaliser et persister les lignes valides.",
               "Retourner le statut et les compteurs de qualité."],
        alternatives=["Opérateur absent : rejet de la requête.", "Structure invalide : statut FAILED.",
                      "Quelques lignes invalides : statut PARTIAL_SUCCESS.", "Fichier déjà importé : rejet avec l’import existant."],
        postconditions="L’import et les transactions Banque sont traçables et disponibles pour un run.",
        api="POST /api/imports/bank ; GET /api/imports/{id}/errors",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-02", title="Importer un fichier opérateur", actor="Agent de rapprochement",
        objective="Charger les transactions MOOV ou Orange à rapprocher avec la Banque.",
        preconditions="Fichier opérateur disponible et date métier connue.",
        trigger="L’utilisateur lance l’import MOOV ou Orange.",
        steps=["Identifier le parseur à partir de l’extension.", "Contrôler les colonnes obligatoires.",
               "Normaliser les identifiants, statuts, montants et dates.", "Écarter les doublons et lignes invalides.",
               "Persister les transactions et les compteurs."],
        alternatives=["Receipt MOOV trop court : ligne invalide.", "Compte ou date Orange manquant : ligne invalide.",
                      "Aucune ligne valide : import FAILED."],
        postconditions="Les transactions opérateur sont disponibles pour la stratégie correspondante.",
        api="POST /api/imports/moov ; POST /api/imports/orange",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-03", title="Importer AMPLITUDE", actor="Contrôle comptable",
        objective="Alimenter la base des écritures utilisées pour vérifier la comptabilisation.",
        preconditions="Export AMPLITUDE lisible et date métier connue.",
        trigger="Le comptable dépose un export AMPLITUDE.",
        steps=["Détecter la ligne d’en-tête.", "Extraire référence, téléphone, compte, montant, date et direction.",
               "Ignorer les lignes de rapport non transactionnelles.", "Persister les écritures valides."],
        alternatives=["Libellé obligatoire absent : import refusé.", "Téléphone absent sur une ligne transactionnelle : ligne invalide."],
        postconditions="Les écritures sont disponibles pour le contrôle comptable et le cas paiement généré.",
        api="POST /api/imports/amplitude",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-04", title="Prévisualiser et supprimer un import", actor="Superviseur ou exploitant",
        objective="Nettoyer un import sans laisser de résultats ou runs incohérents.",
        preconditions="Source et date identifiées ; utilisateur autorisé à supprimer.",
        trigger="L’utilisateur demande l’impact d’une suppression.",
        steps=["Construire un plan contenant imports, transactions, runs et résultats impactés.",
               "Afficher les compteurs et identifiants concernés.", "Exiger confirmCascade=true si un run utilise l’import.",
               "Supprimer résultats, runs, transactions, métadonnées puis fichier physique.",
               "Retourner un bilan identique au plan prévisualisé."],
        alternatives=["Sans confirmation alors qu’un run est impacté : HTTP 409, aucune suppression.",
                      "Aucun import candidat : bilan nul.", "Import Banque : opérateur obligatoire pour les suppressions par périmètre."],
        postconditions="Aucune donnée de réconciliation ne référence l’import supprimé.",
        api="GET /api/imports/{source}/preview-delete ; DELETE /api/imports/{source}?confirmCascade=true",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-05", title="Lancer une réconciliation", actor="Agent de rapprochement",
        objective="Comparer les transactions Banque et opérateur sur une période.",
        preconditions="Imports Banque et opérateur disponibles ; libellé de run renseigné.",
        trigger="L’utilisateur soumet une demande de run.",
        steps=["Résoudre l’opérateur et la période.", "Créer le run RUNNING et mémoriser les imports.",
               "Choisir la stratégie MOOV ou Orange.", "Regrouper les lignes par clé et détecter les doublons.",
               "Classifier et persister chaque résultat.", "Calculer la synthèse et terminer le run."],
        alternatives=["Aucun import : le comportement actuel peut produire un run vide.",
                      "Statut inconnu : résultat STATUT_INCONNU.", "Doublon : résultat prioritaire DOUBLON_BANQUE ou DOUBLON_MOOV."],
        postconditions="Un run COMPLETED et ses résultats sont consultables.",
        api="POST /api/reconciliations/run",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-06", title="Analyser une anomalie", actor="Agent de rapprochement",
        objective="Identifier la cause et l’impact financier d’un résultat non conforme.",
        preconditions="Un run contient des résultats.", trigger="L’utilisateur ouvre une vue d’anomalies ou lance une recherche.",
        steps=["Filtrer par période, opérateur, type ou identifiant métier.", "Consulter statuts, montants, écart et raison.",
               "Retrouver les transactions source associées.", "Exporter le résultat si une analyse externe est requise."],
        alternatives=["Filtres avancés sans correspondance : page vide.", "Identifiant source supprimé : détail partiel conservé dans le résultat."],
        postconditions="L’anomalie est documentée pour traitement métier externe.",
        api="GET /api/reconciliations/results ; GET /runs/{id}/{vue} ; exports CSV/XLSX",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-07", title="Consulter le tableau de bord", actor="Superviseur",
        objective="Piloter la performance et la qualité des rapprochements.",
        preconditions="Canal et période définis.", trigger="Le superviseur ouvre le dashboard.",
        steps=["Sélectionner le dernier run du canal ou le run demandé.", "Calculer KPI financiers sur les résultats pertinents.",
               "Afficher distribution, montants, timeline et top anomalies.",
               "Afficher les indicateurs qualité, dont les opérations opérateur hors périmètre."],
        alternatives=["Filtres incohérents : HTTP 400.", "Aucun run : indicateurs vides ou nuls."],
        postconditions="Une vue consolidée et explicable est disponible.",
        api="GET /api/dashboard/reconciliation/*",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-08", title="Produire un reporting", actor="Superviseur ou contrôle comptable",
        objective="Produire une synthèse périodique partageable.",
        preconditions="Canal, type de période et date de référence fournis.", trigger="L’utilisateur demande un rapport.",
        steps=["Résoudre la fenêtre DAY, WEEK ou MONTH.", "Enrichir les résultats par les dates transactionnelles.",
               "Calculer KPI, distribution, ventilation journalière et détails.", "Générer la réponse JSON, Excel ou PDF."],
        alternatives=["Période sans résultat : rapport vide avec KPI à zéro.", "Erreur de génération : exception applicative."],
        postconditions="Le rapport est consultable ou téléchargé.",
        api="GET /api/dashboard/reconciliation/reporting/summary et /export/{excel|pdf}",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-09", title="Contrôler AMPLITUDE", actor="Contrôle comptable",
        objective="Vérifier que les opérations Banque attendues sont comptabilisées.",
        preconditions="Transactions Banque en succès et écritures AMPLITUDE importées.", trigger="Le comptable demande le contrôle d’une période.",
        steps=["Sélectionner les opérations Banque candidates.", "Rechercher d’abord la référence AMPLITUDE.",
               "Appliquer le fallback date ±1 jour, montant et compte.", "Classer COMPTABILISE ou NON_COMPTABILISE.",
               "Calculer le risque et les écarts."],
        alternatives=["Paiement généré absent d’AMPLITUDE : exclu du périmètre.", "Référence absente : utiliser uniquement le fallback."],
        postconditions="Les lignes et KPI comptables sont disponibles.",
        api="GET /api/accounting/check ; GET /api/accounting/kpi",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-10", title="Analyser la compensation", actor="Responsable compensation",
        objective="Comparer les positions de succès Banque et opérateur.",
        preconditions="Transactions normalisées présentes sur la période.", trigger="L’utilisateur choisit jour, semaine ou mois.",
        steps=["Agréger volumes et montants de succès par canal.", "Calculer Banque moins opérateur.",
               "Retourner OK_COMPENSATION si l’écart est nul, sinon A_VERIFIER.", "Permettre la consultation des écarts détaillés."],
        alternatives=["Aucune opération : montants nuls.", "Plusieurs canaux sans filtre journalier : une ligne par canal et date."],
        postconditions="La position financière et la décision sont disponibles.",
        api="GET /api/compensations/daily|weekly|monthly|discrepancies",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-11", title="Exécuter la rétention", actor="Exploitant",
        objective="Maîtriser le volume et la durée de conservation des réconciliations.",
        preconditions="Tables d’archives disponibles ; politique de conservation validée.", trigger="Planification cron ou appel manuel.",
        steps=["Calculer la date limite.", "Archiver les résultats puis les supprimer par lots.",
               "Archiver les runs devenus orphelins puis les supprimer.", "Retourner les compteurs archivés et purgés."],
        alternatives=["Mode PURGE_ONLY : supprimer sans archive.", "Rétention désactivée : le cron ne traite rien."],
        postconditions="Les données anciennes sont archivées ou purgées selon la politique.",
        api="POST /api/reconciliations/retention/run",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )
    add_use_case_spec(
        doc, uc_id="UC-12", title="Administrer les accès", actor="Administrateur",
        objective="Gérer les identités, profils et autorités d’accès.",
        preconditions="Administrateur authentifié et habilité.", trigger="Création, modification, activation ou suppression d’un compte.",
        steps=["Créer ou modifier l’utilisateur.", "Affecter un profil et ses rôles.", "Activer le compte ou réinitialiser son mot de passe.",
               "Journaliser les actions sensibles."],
        alternatives=["Identifiant déjà utilisé : rejet.", "Ancien mot de passe invalide : changement refusé.",
                      "Profil ou rôle encore référencé : appliquer la règle de suppression définie."],
        postconditions="Les droits effectifs de l’utilisateur sont mis à jour.",
        api="/api/users, /api/profils, /api/roles, /api/authenticate",
        decimal_num_id=decimal_num_id, bullet_num_id=bullet_num_id,
    )

    add_heading(doc, "6.3 Scénario nominal — rapprochement quotidien", 2)
    add_list(doc, [
        "L’agent choisit le canal MOOV ou Orange et la date métier.",
        "Il importe le fichier Banque en précisant la portée opérateur.",
        "Il importe le fichier opérateur correspondant et vérifie les compteurs valides/invalides.",
        "Il lance un run avec un libellé explicite et la même date.",
        "Le service rapproche les clés, classe et persiste les résultats.",
        "L’agent consulte la synthèse, traite d’abord les anomalies financières et exporte si nécessaire.",
        "Le superviseur consulte le tableau de bord et le rapport périodique.",
    ], decimal_num_id)

    add_heading(doc, "6.4 Scénarios alternatifs transverses", 2)
    add_table(doc, ["Situation", "Comportement actuel", "Attendu de recette"], [
        ("Fichier non conforme", "Import FAILED, message de colonnes manquantes.", "Aucune transaction persistée."),
        ("Même fichier déjà importé", "Exception métier si les transactions existent.", "Message avec source/date/portée/importId."),
        ("Quelques lignes invalides", "PARTIAL_SUCCESS.", "Compteurs cohérents et avertissements consultables."),
        ("Aucun import sur la période", "Le run peut être COMPLETED sans résultat.", "Décision métier à confirmer : avertir ou bloquer."),
        ("Suppression d’un import utilisé", "HTTP 409 sans confirmation ; cascade avec confirmCascade=true.", "Le bilan final doit correspondre à la prévisualisation."),
        ("Écart de compensation", "A_VERIFIER.", "Détail des écarts accessible."),
    ], [2300, 3540, 3520], font_size=9.0, first_col_bold=True)

    add_heading(doc, "6.5 Diagrammes d’activité", 2)
    add_picture(doc, activity_import_diagram, alt_text="Diagramme d’activité du processus d’import")
    add_caption(doc, "Figure 3 — Décisions et états du traitement d’un fichier importé.")
    add_picture(doc, activity_reconciliation_diagram, alt_text="Diagramme d’activité du processus de réconciliation")
    add_caption(doc, "Figure 4 — Orchestration d’un run MOOV ou Orange.")

    add_heading(doc, "6.6 Diagrammes de séquence", 2)
    add_picture(doc, sequence_import_diagram, alt_text="Diagramme de séquence de l’import")
    add_caption(doc, "Figure 5 — Collaboration des composants pendant un import.")
    add_picture(doc, sequence_reconciliation_diagram, alt_text="Diagramme de séquence de la réconciliation")
    add_caption(doc, "Figure 6 — Collaboration des composants pendant un run.")
    add_picture(doc, sequence_deletion_diagram, alt_text="Diagramme de séquence de la suppression sécurisée")
    add_caption(doc, "Figure 7 — Prévisualisation, confirmation et cascade d’une suppression.")

    start_section(doc, "7. Conception UML et architecture technique")
    add_heading(doc, "7.1 Diagramme de composants", 2)
    add_picture(doc, architecture_diagram, alt_text="Architecture logique en couches du service")
    add_caption(doc, "Figure 8 — Architecture en couches et composants transverses.")
    add_heading(doc, "7.2 Stack", 2)
    add_table(doc, ["Couche", "Technologies"], [
        ("Runtime", "Java 21, Spring Boot 3.5.11, Maven"),
        ("API", "Spring Web, Validation, Springdoc OpenAPI"),
        ("Sécurité", "Spring Security, JWT JJWT 0.11.5, BCrypt"),
        ("Données", "Spring Data JPA, PostgreSQL 16, Flyway"),
        ("Fichiers", "Apache POI 5.4.1, Commons CSV 1.13.0"),
        ("Rapports", "PDFBox 3.0.3, JasperReports 6.19.1, POI"),
        ("Support", "Lombok, MapStruct, Thymeleaf, Mail"),
    ], [2000, 7360], font_size=9.4, first_col_bold=True)

    add_heading(doc, "7.3 Découpage applicatif", 2)
    add_table(doc, ["Package", "Responsabilité"], [
        ("controller", "Contrats HTTP, validation des paramètres, pagination et formats d’export."),
        ("service / impl", "Orchestration des cas d’utilisation et calculs métier."),
        ("service.reconciliation", "Stratégies opérateur et écriture des résultats."),
        ("service.parser", "Lecture et validation des formats CSV/XLS/XLSX."),
        ("model", "Entités JPA de réconciliation et backoffice."),
        ("repositories", "Accès aux données, agrégats et filtres."),
        ("security / jwt", "Authentification, utilisateurs et token JWT."),
        ("config", "Profils, sécurité, CORS, OpenAPI, audit et rétention."),
        ("dto / enums", "Contrats d’échange et vocabulaires métier."),
    ], [2600, 6760], font_size=9.2, first_col_bold=True)

    add_heading(doc, "7.4 Principes de conception observés", 2)
    add_list(doc, [
        "Strategy pattern pour isoler les particularités MOOV et Orange.",
        "Pipeline d’import commun avec parseurs polymorphes par extension.",
        "Normalisation des valeurs externes avant application des règles métier.",
        "Persistance d’un résultat matérialisé pour faciliter consultation, reporting et audit.",
        "Flyway comme source de vérité de l’évolution du schéma.",
        "Traitements destructifs et de rétention exécutés sous transaction et par lots.",
    ], bullet_num_id)
    add_callout(doc, "Évolutivité", "L’ajout d’un nouvel opérateur est conceptuellement facilité par ReconciliationOperatorStrategy, mais exige encore des adaptations du schéma commun, des DTO, du dashboard, du reporting et des endpoints.")

    add_heading(doc, "7.5 Diagramme de classes", 2)
    add_picture(doc, class_diagram, alt_text="Diagramme UML des classes du domaine de réconciliation")
    add_caption(doc, "Figure 9 — Classes principales, attributs et associations du domaine.")
    add_table(doc, ["Association", "Cardinalité", "Interprétation"], [
        ("FileImport → transaction source", "1 à 0..*", "Un import possède plusieurs lignes d’une seule source."),
        ("ReconciliationRun → ReconciliationResult", "1 à 0..*", "Un run matérialise un résultat par clé ou doublon."),
        ("ReconciliationResult → BankTransaction", "0..1", "La transaction Banque peut être absente."),
        ("ReconciliationResult → transaction opérateur", "0..1", "La transaction MOOV ou Orange peut être absente."),
        ("AmplitudeTransaction", "indépendante du run", "Utilisée par recherche de référence pour la comptabilisation et certains matchs."),
    ], [3000, 1500, 4860], font_size=9.0, first_col_bold=True)

    add_heading(doc, "7.6 Diagrammes d’états", 2)
    add_picture(doc, state_diagram, alt_text="Diagrammes d’états de FileImport et ReconciliationRun")
    add_caption(doc, "Figure 10 — Cycles de vie des imports et runs de réconciliation.")
    add_callout(doc, "Observation", "L’enum ReconciliationRunStatus prévoit FAILED, mais le workflow transactionnel actuel ne persiste pas systématiquement cet état lorsqu’une exception interrompt le run. Le diagramme distingue donc l’état implémenté de la cible de robustesse.", fill="FFF7E6", color=GOLD)

    add_heading(doc, "7.7 Diagramme de déploiement", 2)
    add_picture(doc, deployment_diagram, alt_text="Diagramme de déploiement cible de la plateforme")
    add_caption(doc, "Figure 11 — Nœuds d’exécution, dépendances et flux techniques.")
    add_body(doc, "Le déploiement cible sépare clairement le client, le service Spring Boot, PostgreSQL, le stockage des fichiers, les services de messagerie et la supervision. Le port publié doit être aligné avec server.port, et les secrets doivent provenir de l’environnement ou d’un gestionnaire dédié.")

    start_section(doc, "8. Modèle de données")
    add_picture(doc, data_diagram, alt_text="Modèle de données simplifié de la réconciliation")
    add_caption(doc, "Figure 12 — Entités principales et relations fonctionnelles.")
    add_heading(doc, "8.1 Entités principales", 2)
    add_table(doc, ["Entité", "Rôle", "Clés / relations"], [
        ("file_import", "Métadonnée, qualité et traçabilité du fichier.", "1–N vers transactions ; source, opérateur, date, checksum."),
        ("bank_transaction", "Ligne Banque/Carthago normalisée.", "N–1 import ; transactionId comme clé métier."),
        ("moov_transaction", "Ligne MOOV normalisée.", "N–1 import ; receiptNo comme clé métier."),
        ("orange_transaction", "Ligne Orange normalisée.", "N–1 import ; omTransactionId comme clé métier."),
        ("amplitude_transaction", "Écriture comptable importée.", "N–1 import ; référence opération utilisée au matching."),
        ("reconciliation_run", "Exécution par opérateur et période.", "Référence les IDs d’import sous forme de chaînes CSV."),
        ("reconciliation_result", "Résultat matérialisé par clé.", "N–1 run ; IDs Banque/opérateur non matérialisés en FK."),
        ("*_archive", "Copies historiques avant purge.", "Source IDs uniques ; fidélité partielle du run courant."),
        ("ba_utilisateur/profil/role", "Identités et habilitations legacy.", "Utilisateur N–1 profil ; profil N–N rôles."),
    ], [2350, 3670, 3340], font_size=8.8, first_col_bold=True)

    add_heading(doc, "8.2 Contraintes et index", 2)
    add_list(doc, [
        "Index sur clés de rapprochement, imports, dates métier, types de résultat et opérateur/date de run.",
        "Index unique anti-réimport sur source + date métier + portée opérateur + checksum.",
        "Relations transaction → import et résultat → run ; AMPLITUDE utilise ON DELETE CASCADE, les premières tables non.",
        "Références transactionnelles des résultats stockées comme IDs simples afin de tolérer un schéma opérateur commun.",
        "Champs rawPayloadJson et reason en TEXT pour diagnostic et traçabilité.",
    ], bullet_num_id)
    add_heading(doc, "8.3 Dette de modèle identifiée", 2)
    add_body(doc, "Les colonnes moov_transaction_id, moov_status_raw et moov_amount contiennent aussi les valeurs Orange. Ce choix facilite la réutilisation des vues mais rend le modèle ambigu. Les listes d’imports sont stockées en texte CSV dans le run plutôt que dans une table d’association. Les archives de run ne reprennent pas explicitement operator ni orange_import_ids, ce qui peut réduire la fidélité historique.")

    start_section(doc, "9. Catalogue des API REST")
    add_body(doc, "Base fonctionnelle : /api. En configuration principale, le serveur écoute le port 8087. Les tableaux ci-dessous synthétisent les contrats exposés ; la pagination Spring est utilisée pour les collections Page.")

    add_heading(doc, "9.1 Authentification et administration", 2)
    admin_endpoints = [
        ("POST", "/api/authenticate", "Authentifier et obtenir un JWT."),
        ("GET/POST", "/api/users", "Lister ou créer un utilisateur."),
        ("PUT/DELETE", "/api/users/{code}", "Modifier ou supprimer un utilisateur."),
        ("GET", "/api/users/details", "Informations de l’utilisateur courant."),
        ("PUT", "/api/users/change-password", "Changer le mot de passe."),
        ("PUT", "/api/users/request-reset-password", "Demander une réinitialisation."),
        ("PUT", "/api/users/complete-reset-password", "Finaliser la réinitialisation."),
        ("GET", "/api/users/{id}/activate", "Activer un utilisateur."),
        ("GET/POST/PUT/DELETE", "/api/roles[/{id}]", "CRUD des rôles."),
        ("GET/POST/PUT/DELETE", "/api/profils[/{id}]", "CRUD des profils."),
        ("GET", "/api/csrf", "Retourner le token CSRF."),
        ("GET", "/api/documents/{id}", "Charger un document binaire."),
        ("GET/POST/PUT/DELETE", "/api/categories[/{id}]", "CRUD catégories."),
        ("GET/POST/PUT/DELETE", "/api/products[/{id}]", "CRUD produits."),
        ("GET", "/api/reporting/products", "Rapport Jasper produits (legacy)."),
    ]
    add_table(doc, ["Méthode", "Chemin", "Finalité"], admin_endpoints, [1250, 4050, 4060], font_size=8.6, first_col_bold=True)

    add_heading(doc, "9.2 Imports et transactions", 2)
    import_endpoints = [
        ("POST", "/api/imports/bank", "Importer Banque ; file, operator, businessDate."),
        ("POST", "/api/imports/moov", "Importer MOOV ; file, businessDate."),
        ("POST", "/api/imports/orange", "Importer Orange ; file, businessDate."),
        ("POST", "/api/imports/amplitude", "Importer AMPLITUDE ; file, businessDate."),
        ("GET", "/api/imports", "Lister les imports paginés."),
        ("GET", "/api/imports/{id}", "Consulter un import."),
        ("GET", "/api/imports/{id}/errors", "Consulter le message d’erreur/avertissement."),
        ("DELETE", "/api/imports/{source}/latest", "Supprimer le dernier import de la source."),
        ("DELETE", "/api/imports/{source}", "Supprimer par source/date/portée ; confirmation requise si cascade."),
        ("DELETE", "/api/imports/{source}/all", "Supprimer tous les imports ; confirmCascade requis si un run est impacté."),
        ("GET", "/api/imports/{source}/preview-delete", "Prévisualiser IDs et volumes avant suppression."),
        ("GET", "/api/transactions/{bank|moov|orange}", "Lister par importId."),
        ("GET/POST", "/api/transactions/{source}/by-ids", "Charger par identifiants."),
    ]
    add_table(doc, ["Méthode", "Chemin", "Finalité"], import_endpoints, [1250, 4050, 4060], font_size=8.6, first_col_bold=True)

    add_heading(doc, "9.3 Réconciliation", 2)
    rec_endpoints = [
        ("POST", "/api/reconciliations/run", "Créer et exécuter un run."),
        ("GET", "/api/reconciliations/runs", "Lister/filtrer les runs."),
        ("GET", "/api/reconciliations/runs/{id}", "Détail d’un run."),
        ("GET", "/api/reconciliations/runs/{id}/results", "Résultats paginés du run."),
        ("GET", "/api/reconciliations/runs/{id}/summary", "Synthèse du run."),
        ("GET", "/api/reconciliations/summary", "Synthèse globale ou du dernier run opérateur."),
        ("GET", "/api/reconciliations/results", "Recherche globale et filtres avancés."),
        ("GET", "/api/reconciliations/runs/{id}/{vue}", "Vues matches, débits, crédits, échecs, absents, doublons, écarts."),
        ("GET", "/api/reconciliations/history/by-date", "Historique d’un jour."),
        ("GET", "/api/reconciliations/history/range", "Historique d’une plage."),
        ("GET", "/api/reconciliations/history/latest", "Dernier run."),
        ("GET", "/api/reconciliations/history/reset", "Historique des 7 derniers jours."),
        ("GET", "/api/reconciliations/runs/{id}/export/csv", "Export CSV minimal."),
        ("GET", "/api/reconciliations/runs/{id}/export/xlsx", "Export XLSX détaillé."),
    ]
    add_table(doc, ["Méthode", "Chemin", "Finalité"], rec_endpoints, [1250, 4050, 4060], font_size=8.6, first_col_bold=True)

    add_heading(doc, "9.4 Pilotage, finance et exploitation", 2)
    other_endpoints = [
        ("GET", "/api/dashboard/reconciliation/summary", "Synthèse dashboard."),
        ("GET", "/api/dashboard/reconciliation/results-distribution", "Distribution des résultats."),
        ("GET", "/api/dashboard/reconciliation/amounts", "Montants agrégés."),
        ("GET", "/api/dashboard/reconciliation/timeline", "Timeline horaire."),
        ("GET", "/api/dashboard/reconciliation/top-anomalies", "Anomalies triées par montant."),
        ("GET", "/api/dashboard/reconciliation/data-quality", "Qualité des imports."),
        ("GET", "/api/dashboard/reconciliation/reporting/summary", "Rapport DAY/WEEK/MONTH."),
        ("GET", "/api/dashboard/reconciliation/reporting/export/{excel|pdf}", "Exports reporting."),
        ("GET", "/api/accounting/check", "Contrôle comptable multi-opérateur."),
        ("GET", "/api/accounting/amplitude/bank{Moov|Orange}/{check|kpi}", "Contrôle/KPI par canal."),
        ("GET", "/api/accounting/{...}/kpi/export/{csv|pdf}", "Exports KPI comptables."),
        ("GET", "/api/accounting/carthago/latest-date", "Dernière date Banque en succès."),
        ("DELETE", "/api/accounting/amplitude/cleanup/{daily|range|weekly|all}", "Nettoyage AMPLITUDE."),
        ("GET", "/api/compensations/{daily|weekly|monthly}", "Positions de compensation."),
        ("GET", "/api/compensations/weekly/by-reference", "Semaine contenant une date."),
        ("GET", "/api/compensations/discrepancies", "Détail paginé des écarts."),
        ("POST", "/api/reconciliations/retention/run", "Exécuter la rétention."),
    ]
    add_table(doc, ["Méthode", "Chemin", "Finalité"], other_endpoints, [1250, 4050, 4060], font_size=8.4, first_col_bold=True)

    start_section(doc, "10. Sécurité, configuration et exploitation")
    add_heading(doc, "10.1 Profils de sécurité", 2)
    add_table(doc, ["Profil", "Comportement"], [
        ("dev", "Toutes les requêtes sont autorisées ; CORS et CSRF désactivés."),
        ("prod", "Sessions stateless, JWT avant UsernamePasswordAuthenticationFilter, BCrypt, CSRF désactivé, endpoints /api authentifiés sauf exceptions."),
    ], [1800, 7560], font_size=9.4, first_col_bold=True)
    add_heading(doc, "10.2 Accès publics constatés en production", 2)
    add_list(doc, [
        "Swagger/OpenAPI.",
        "Authentification, activation et routes de reset.",
        "Le chemin exact /api/users est déclaré permitAll sans restriction de méthode : GET et POST sont donc publics à confirmer.",
        "GET /api/categories est public ; GET /api/products exige BA_ADMIN.",
        "Les autres routes /api/** exigent seulement une authentification globale ; aucune annotation d’autorisation fine n’est présente.",
    ], bullet_num_id)
    add_heading(doc, "10.3 Configuration", 2)
    add_table(doc, ["Paramètre", "Valeur / rôle actuel"], [
        ("server.port", "8087 dans application.yml."),
        ("spring.profiles.active", "prod par défaut."),
        ("multipart", "50 MB par fichier, 60 MB par requête."),
        ("app.storage.path", "./datas, stockage local."),
        ("app.reconciliation.amount-tolerance", "Tolérance des écarts MOOV."),
        ("app.retention", "Désactivée ; 180 jours ; lots de 5 000 ; 02 h 30 ; archive puis purge."),
        ("app.cors.allowed-origins", "Origines configurables pour /api/**."),
    ], [3100, 6260], font_size=9.2, first_col_bold=True)
    add_heading(doc, "10.4 Déploiement", 2)
    add_body(doc, "Le projet fournit un Dockerfile Java 21 et un docker-compose avec PostgreSQL 16. L’image copie target/reconcilliation-service-1.0.0.jar. La configuration Docker expose et publie actuellement le port 8083 alors que l’application principale écoute 8087 : le service ne sera pas accessible via le mapping prévu sans correction ou variable SERVER_PORT.")
    add_callout(doc, "Sécurité d’exploitation", "Des informations sensibles de connexion sont présentes dans des fichiers de configuration versionnés. Elles doivent être externalisées vers des variables d’environnement ou un gestionnaire de secrets, puis renouvelées.", fill="FFF1F1", color=RED)
    add_heading(doc, "10.5 Exigences non fonctionnelles cibles", 2)
    add_table(doc, ["Domaine", "Exigence proposée"], [
        ("Disponibilité", "Sauvegarde PostgreSQL, supervision Actuator, alertes import/run en échec."),
        ("Performance", "Pagination systématique, batch sur gros fichiers, index mesurés, éviter findAll AMPLITUDE à grande volumétrie."),
        ("Sécurité", "RBAC par fonctionnalité, secrets externes, CORS explicite, limitation des tentatives, logs sans données sensibles."),
        ("Audit", "Journaliser imports, suppressions, runs, exports, rétention et décisions utilisateur."),
        ("Résilience", "Statut FAILED persistant, reprise contrôlée, idempotence des runs, nettoyage des fichiers orphelins."),
        ("Protection des données", "Durée de conservation validée, minimisation des raw payloads, chiffrement et contrôle d’accès."),
        ("Interopérabilité", "Contrats OpenAPI versionnés et formats d’erreurs homogènes."),
    ], [1900, 7460], font_size=9.0, first_col_bold=True)

    start_section(doc, "11. Tests, recette et qualité")
    add_heading(doc, "11.1 État des tests", 2)
    add_callout(doc, "Résultat vérifié", "mvn test : 28 tests exécutés, 0 échec, 0 erreur, 0 ignoré. Build SUCCESS le 22 juin 2026.", fill="EAF4EE", color=GREEN)
    add_table(doc, ["Zone", "Couverture observée"], [
        ("Normalisation", "Statuts Banque, MOOV et Orange."),
        ("Classification", "MATCH_OK, écart MOOV, Orange, opérateur succès/échec sans Banque."),
        ("Import", "Formats Banque, directions, extraction MOOV, montants AMPLITUDE, dédoublonnage."),
        ("Résultats", "Résolution de la date métier Banque/Orange."),
        ("Dashboard", "Synthèse, dernier run et validation de filtres."),
        ("Intégration", "Classes IT avec fichiers d’exemple, non exécutées par mvn test par défaut."),
    ], [2200, 7160], font_size=9.3, first_col_bold=True)
    add_heading(doc, "11.2 Scénarios de recette prioritaires", 2)
    acceptance = [
        ("REC-01", "Import Banque sans opérateur", "Rejet 400 avec message explicite."),
        ("REC-02", "Réimport du même fichier", "Blocage et conservation de l’import initial."),
        ("REC-03", "MOOV avec lignes invalides", "PARTIAL_SUCCESS et compteurs exacts."),
        ("REC-04", "Orange avec séparateur non attendu", "Erreur maîtrisée et diagnostiquable."),
        ("REC-05", "Deux succès, montants égaux", "MATCH_OK pour chaque canal."),
        ("REC-06", "Deux succès, montants différents", "Résultat harmonisé attendu selon règle validée."),
        ("REC-07", "Opérateur échec sans Banque", "Exclu des KPI financiers ; exposé par operatorOutOfScopeCount/rate et le reporting hors périmètre."),
        ("REC-08", "Paiement généré avec/sans AMPLITUDE", "Classification et périmètre comptable conformes."),
        ("REC-09", "Run sans import", "Comportement métier validé : blocage ou run vide signalé."),
        ("REC-10", "Suppression import utilisé", "HTTP 409 sans confirmation ; avec confirmCascade, le bilan supprimé égale la prévisualisation."),
        ("REC-11", "Rétention archive puis purge", "Volumes et données archivées identiques à la source."),
        ("REC-12", "JWT expiré / rôle insuffisant", "401/403 homogènes."),
        ("REC-13", "Volumétrie élevée", "Temps et mémoire dans les seuils convenus."),
        ("REC-14", "Port de conteneur", "Health check accessible via le port publié."),
    ]
    add_table(doc, ["ID", "Scénario", "Résultat attendu"], acceptance, [1000, 3560, 4800], font_size=8.8, first_col_bold=True)
    add_heading(doc, "11.3 Critères d’acceptation globaux", 2)
    add_list(doc, [
        "Chaque fichier accepté est traçable jusqu’aux lignes et aux erreurs.",
        "Chaque run indique clairement son canal, sa période, ses imports et son issue.",
        "Les catégories de résultat sont déterministes et identiques pour des situations métier équivalentes.",
        "Les KPI sont reproductibles à partir des résultats détaillés.",
        "Toute suppression ou purge fournit un bilan et respecte les dépendances.",
        "Les endpoints sensibles sont protégés par une autorisation explicite.",
    ], bullet_num_id)

    start_section(doc, "12. Écarts, risques et recommandations")
    risks = [
        ("R01", "Critique", "Ports 8087/8083 incohérents entre application et Docker.", "Aligner EXPOSE, mapping et SERVER_PORT ; ajouter un health check."),
        ("R02", "Critique", "Secrets présents dans la configuration versionnée.", "Externaliser, renouveler et scanner l’historique Git."),
        ("R03", "Élevé", "GET/POST /api/users potentiellement publics en production.", "Restreindre par méthode et clarifier le parcours d’inscription."),
        ("R04", "Élevé", "Pas d’autorisation métier fine par module.", "Définir RBAC et ajouter @PreAuthorize ou règles HTTP explicites."),
        ("R05", "Élevé", "Tolérance montant MOOV non appliquée à Orange.", "Introduire une règle commune opérateur et des tests croisés."),
        ("R06", "Élevé", "Asymétries Banque seule entre MOOV et Orange.", "Valider la matrice métier et harmoniser les stratégies."),
        ("R07", "Élevé", "Request de run peu validée ; IDs d’import fournis mais ignorés.", "Valider date/plage, imports et opérateur ; supprimer ou utiliser les champs."),
        ("R08", "Élevé", "Run vide peut être marqué COMPLETED.", "Bloquer ou produire un statut/avertissement explicite."),
        ("R09", "Moyen", "summaryJson contient toString() et non du JSON.", "Sérialiser avec Jackson dans une structure versionnée."),
        ("R10", "Moyen", "Endpoint doublons retourne seulement DOUBLON_BANQUE.", "Retourner Banque + opérateur ou séparer deux vues."),
        ("R11", "Moyen", "Recherche avancée globale ne reprend pas la sémantique du dernier run.", "Unifier le filtrage afin d’éviter les doublons inter-runs."),
        ("R12", "Moyen", "Champs moov_* réutilisés pour Orange.", "Renommer en operator_* ou normaliser le modèle."),
        ("R13", "Moyen", "Archives de run incomplètes pour operator/orange_import_ids.", "Faire évoluer le schéma et le SQL d’archivage."),
        ("R14", "Moyen", "cutoff nommé exclusive mais SQL utilise <=.", "Clarifier le contrat et tester la date frontière."),
        ("R15", "Moyen", "Reporting/comptabilisation chargent des ensembles larges en mémoire.", "Ajouter des requêtes ciblées et mesures de volumétrie."),
        ("R16", "Moyen", "Tests IT non intégrés au build par défaut.", "Configurer Failsafe verify et un profil CI."),
        ("R17", "Faible", "README indique encore XLSX comme placeholder et un autre port.", "Mettre la documentation sous contrôle de version avec revue."),
    ]
    add_table(doc, ["ID", "Priorité", "Risque / écart", "Recommandation"], risks, [700, 1100, 3610, 3950], font_size=8.1, first_col_bold=True)

    start_section(doc, "13. Feuille de route et traçabilité")
    add_heading(doc, "13.1 Feuille de route proposée", 2)
    roadmap = [
        ("Lot 0 — Sécurisation", "1 à 2 semaines", "Ports, secrets, règles publiques, RBAC minimal, validation des runs."),
        ("Lot 1 — Cohérence métier", "2 à 3 semaines", "Matrice commune MOOV/Orange, tolérance montant, doublons, tests de décision."),
        ("Lot 2 — Données et audit", "2 à 4 semaines", "Schéma operator_*, associations d’import, JSON de synthèse, archives fidèles."),
        ("Lot 3 — Industrialisation", "2 à 4 semaines", "CI avec IT, health checks, métriques, performances, sauvegarde et reprise."),
        ("Lot 4 — Workflow métier", "Selon besoin", "Affectation, commentaire, justification, validation et clôture des anomalies."),
    ]
    add_table(doc, ["Lot", "Horizon", "Contenu"], roadmap, [2100, 1800, 5460], font_size=9.2, first_col_bold=True)
    add_heading(doc, "13.2 Décisions à faire valider", 2)
    add_list(doc, [
        "Matrice de classification unique ou règles explicitement différentes par opérateur.",
        "Comportement attendu lorsqu’un run ne trouve aucun import ou aucune transaction.",
        "Règle d’idempotence et politique de conservation de plusieurs runs sur le même périmètre.",
        "Rôles autorisés pour importer, supprimer, exécuter la rétention et administrer les utilisateurs.",
        "Durées de conservation des fichiers bruts, raw payloads, résultats et archives.",
        "Seuils de performance, de volumétrie, de tolérance financière et de compensation.",
    ], bullet_num_id)
    add_heading(doc, "13.3 Matrice de traçabilité", 2)
    add_table(doc, ["Exigence", "Cas", "Composant / API", "Preuve ou test"], [
        ("RF-01 / RF-04", "UC-01", "FileImportService, /api/imports/bank", "FileImportServiceImplTest ; BankImportExportMoovFormatIT"),
        ("RF-02 / RF-04", "UC-02", "Parseurs, normalisation, /imports/moov|orange", "StatusNormalizationServiceTest ; fichiers IT"),
        ("RF-03", "UC-03", "AmplitudeTransaction, /imports/amplitude", "Tests d’extraction AMPLITUDE"),
        ("RF-05 / RF-06", "UC-04", "DeletionPlan, preview-delete, confirmCascade", "FileImportDeletionServiceTest"),
        ("RF-07 / RF-08", "UC-05", "ReconciliationService, Strategy, Classification", "ReconciliationClassificationServiceTest ; SampleFilesIT"),
        ("RF-09", "UC-06", "ReconciliationController, exports", "Test de contrat/export recommandé"),
        ("RF-10", "UC-07", "DashboardService, /dashboard/reconciliation", "DashboardServiceImplTest ; DashboardControllerUnitTest"),
        ("RF-11", "UC-08", "ReportingService, exports Excel/PDF", "Tests de reporting à ajouter"),
        ("RF-12", "UC-09", "AccountingService, /api/accounting", "Tests comptables à ajouter"),
        ("RF-13", "UC-10", "CompensationService, /api/compensations", "Tests de compensation à ajouter"),
        ("RF-14", "UC-11", "RetentionService, tables archive", "Tests PostgreSQL/Flyway à ajouter"),
        ("RF-15", "UC-12", "BaUserService, JWT, profils/rôles", "Tests sécurité/RBAC à ajouter"),
    ], [1250, 900, 3520, 3690], font_size=8.2, first_col_bold=True)
    add_body(doc, "Cette matrice sert de contrôle de couverture : toute exigence doit être reliée à un cas d’utilisation, à un composant ou contrat API, puis à une preuve de recette automatisée ou manuelle.")

    add_heading(doc, "13.4 Glossaire", 2)
    add_table(doc, ["Terme", "Définition"], [
        ("Import", "Métadonnée et ensemble de transactions provenant d’un fichier source."),
        ("Run", "Exécution de réconciliation portant sur un opérateur et une période."),
        ("Clé de rapprochement", "Identifiant commun utilisé pour associer une ligne Banque à une ligne opérateur."),
        ("Carthago", "Source bancaire des opérations Banque vers Wallet ou Wallet vers Banque."),
        ("AMPLITUDE", "Système comptable utilisé pour vérifier la comptabilisation."),
        ("Anomalie", "Résultat différent de MATCH_OK et financièrement pertinent."),
        ("Hors périmètre opérateur", "Opération opérateur en échec sans transaction Banque ; suivie en qualité, hors KPI financiers."),
        ("Cascade", "Suppression ordonnée des résultats, runs, transactions, import et fichier dépendants."),
        ("KPI", "Indicateur chiffré de volume, taux, montant, qualité ou risque."),
    ], [2500, 6860], font_size=9.1, first_col_bold=True)

    add_heading(doc, "13.5 Référentiel de preuve", 2)
    add_body(doc, "Principaux chemins inspectés :")
    add_table(doc, ["Domaine", "Chemins source"], [
        ("API", "src/main/java/com/bakouan/app/controller"),
        ("Métier", "src/main/java/com/bakouan/app/service et service/impl"),
        ("Stratégies", "src/main/java/com/bakouan/app/service/reconciliation"),
        ("Parsing", "src/main/java/com/bakouan/app/service/parser"),
        ("Données", "src/main/java/com/bakouan/app/model, repositories et resources/db/migration"),
        ("Sécurité", "src/main/java/com/bakouan/app/config et security"),
        ("Configuration", "pom.xml, application*.yml, Dockerfile, docker-compose.yml"),
        ("Tests", "src/test/java/com/bakouan/app"),
        ("Documentation existante", "README.md, README_SERVICES.md, README_REGLES_METIER.md, CAHIER_DESCRIPTION_FONCTIONNALITES.md"),
    ], [2200, 7160], font_size=9.2, first_col_bold=True)
    add_callout(doc, "Fin du document", "Ce cahier peut servir de base à une validation métier, à la rédaction des spécifications de recette et à la priorisation du backlog technique.")

    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    build_document()
