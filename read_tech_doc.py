import sys
sys.stdout.reconfigure(encoding='utf-8')
from docx import Document
doc = Document('普通直连携程渠道技术设计文档.docx')
for p in doc.paragraphs:
    if p.text.strip():
        print(p.text)
for ti, t in enumerate(doc.tables):
    print(f'\n=== 表格 {ti} ===')
    for row in t.rows:
        cells = [c.text.strip() for c in row.cells]
        print(' | '.join(cells))
