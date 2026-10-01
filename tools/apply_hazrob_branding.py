#!/usr/bin/env python3
from pathlib import Path
import base64

ROOT=Path(__file__).resolve().parents[1]
B64=ROOT/'branding/hazrob_sensing_logo_96.webp.b64'
DRAW=ROOT/'app/src/main/res/drawable/ic_hazrob_app.webp'
JAVA=ROOT/'app/src/main/java/com/servoc/app/branding/HazRobBranding.java'
logo=B64.read_text(encoding='ascii').strip()
DRAW.parent.mkdir(parents=True,exist_ok=True)
DRAW.write_bytes(base64.b64decode(logo))
JAVA.parent.mkdir(parents=True,exist_ok=True)
JAVA.write_text('''package com.servoc.app.branding;\n\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\nimport android.graphics.Canvas;\nimport android.graphics.Paint;\nimport android.graphics.RectF;\nimport android.util.Base64;\nimport java.io.ByteArrayOutputStream;\n\n/** Controlled HazRob Sensing visual identity. */\npublic final class HazRobBranding {\n private HazRobBranding(){}\n public static final int YELLOW=0xFFFFC107, GREY=0xFF616161, LIGHT_GREY=0xFFF5F5F5, WHITE=0xFFFFFFFF, BLACK=0xFF111111;\n private static final String LOGO="'''+logo+'''";\n public static Bitmap logo(){byte[] b=Base64.decode(LOGO,Base64.DEFAULT);return BitmapFactory.decodeByteArray(b,0,b.length);}\n public static byte[] logoPng(){Bitmap b=logo();ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,o);b.recycle();return o.toByteArray();}\n public static void drawPdfHeader(Canvas c){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(YELLOW);c.drawRect(28,8,567,12,p);Bitmap b=logo();c.drawBitmap(b,null,new RectF(505,14,563,72),p);b.recycle();}\n}\n''',encoding='utf-8')

# IMPORTANT: branding must never rewrite application UI palette source files.
# The HR069 UI owns its dark high-contrast palette in UiKit and each workspace. A previous
# implementation replaced dark surfaces with white/light-grey during Gradle preBuild, producing
# an APK whose yellow/grey text was unreadable even though the checked-in Java source was dark.
# Keep this task limited to controlled logo/report identity generation.

# PDF: brand header on every generated legacy report page. Idempotent source edit.
p=ROOT/'app/src/main/java/com/servoc/app/report/ReportExporter.java';s=p.read_text(encoding='utf-8')
if 'com.servoc.app.branding.HazRobBranding' not in s:s=s.replace('import com.servoc.app.ReleaseAudit;','import com.servoc.app.ReleaseAudit;\nimport com.servoc.app.branding.HazRobBranding;')
s=s.replace('private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());}','private static PdfDocument.Page page(PdfDocument pdf,int n){PdfDocument.Page p=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());HazRobBranding.drawPdfHeader(p.getCanvas());return p;}')
p.write_text(s,encoding='utf-8')

# XLSX: include the controlled logo as the first embedded report image and brand summary heading.
p=ROOT/'app/src/main/java/com/servoc/app/report/XlsxReportWriter.java';s=p.read_text(encoding='utf-8')
if 'com.servoc.app.branding.HazRobBranding' not in s:s=s.replace('import com.servoc.app.ReleaseAudit;','import com.servoc.app.ReleaseAudit;\nimport com.servoc.app.branding.HazRobBranding;')
s=s.replace('List<byte[]> charts=renderCharts(series,analytics,chemicals);','List<byte[]> charts=renderCharts(series,analytics,chemicals);charts.add(0,HazRobBranding.logoPng());')
s=s.replace('strRow(x,row++,true,"HazRob "+ReleaseAudit.VERSION,"القيمة");','strRow(x,row++,true,"HazRob SENSING • "+ReleaseAudit.VERSION,"القيمة");')
p.write_text(s,encoding='utf-8')
print('HazRob branding applied without UI palette mutation')
