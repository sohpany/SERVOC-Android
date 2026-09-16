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
JAVA.write_text('''package com.servoc.app.branding;\n\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\nimport android.graphics.Canvas;\nimport android.graphics.Color;\nimport android.graphics.Paint;\nimport android.graphics.RectF;\nimport android.util.Base64;\nimport java.io.ByteArrayOutputStream;\n\n/** HR-0101-062 controlled HazRob Sensing identity. */\npublic final class HazRobBranding {\n private HazRobBranding(){}\n public static final int YELLOW=0xFFFFC107, GREY=0xFF616161, LIGHT_GREY=0xFFF5F5F5, WHITE=0xFFFFFFFF, BLACK=0xFF111111;\n private static final String LOGO="'''+logo+'''";\n public static Bitmap logo(){byte[] b=Base64.decode(LOGO,Base64.DEFAULT);return BitmapFactory.decodeByteArray(b,0,b.length);}\n public static byte[] logoPng(){Bitmap b=logo();ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,o);b.recycle();return o.toByteArray();}\n public static void drawPdfHeader(Canvas c){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(YELLOW);c.drawRect(28,8,567,12,p);Bitmap b=logo();c.drawBitmap(b,null,new RectF(505,14,563,72),p);b.recycle();}\n}\n''',encoding='utf-8')

# Apply the new yellow/grey/white visual system consistently to Java UI sources.
for p in (ROOT/'app/src/main/java/com/servoc/app').rglob('*.java'):
    s=p.read_text(encoding='utf-8')
    s=s.replace('0xFF90CAF9','0xFFFFC107').replace('0xFF245A7A','0xFF616161').replace('0xFF263238','0xFF616161')
    s=s.replace('0xFF0A0D11','0xFFFFFFFF').replace('0xFF181D24','0xFFF5F5F5').replace('0xFF232A33','0xFFF5F5F5')
    s=s.replace('0xFFB8C1CC','0xFF616161').replace('0xFF30363D','0xFFBDBDBD').replace('0xFF607D8B','0xFF616161').replace('0xFF90A4AE','0xFF616161')
    p.write_text(s,encoding='utf-8')

# UiKit defaults: white/light-grey surfaces, dark text, yellow primary buttons.
p=ROOT/'app/src/main/java/com/servoc/app/ui/UiKit.java';s=p.read_text(encoding='utf-8')
s=s.replace('t.setTextColor(Color.WHITE);','t.setTextColor(0xFF111111);')
s=s.replace('public static Button button(Context c,String s){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return b;}','public static Button button(Context c,String s){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextColor(0xFF111111);b.setBackgroundColor(0xFFFFC107);b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return b;}')
s=s.replace('e.setTextColor(Color.WHITE);','e.setTextColor(0xFF111111);')
p.write_text(s,encoding='utf-8')

# Main tabs: selected = yellow/black, unselected = light grey/dark grey.
p=ROOT/'app/src/main/java/com/servoc/app/MainActivity.java';s=p.read_text(encoding='utf-8')
s=s.replace('title.setTextColor(Color.WHITE);','title.setTextColor(0xFF111111);')
s=s.replace('tabButtons[i].setTextColor(i==index?0xFFFFC107:Color.DKGRAY);tabButtons[i].setBackgroundColor(i==index?0xFF616161:0xFFE0E0E0);','tabButtons[i].setTextColor(i==index?0xFF111111:0xFF616161);tabButtons[i].setBackgroundColor(i==index?0xFFFFC107:0xFFF5F5F5);')
p.write_text(s,encoding='utf-8')

# PDF: brand header on every generated page.
p=ROOT/'app/src/main/java/com/servoc/app/report/ReportExporter.java';s=p.read_text(encoding='utf-8')
if 'com.servoc.app.branding.HazRobBranding' not in s:s=s.replace('import com.servoc.app.ReleaseAudit;','import com.servoc.app.ReleaseAudit;\nimport com.servoc.app.branding.HazRobBranding;')
s=s.replace('private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());}','private static PdfDocument.Page page(PdfDocument pdf,int n){PdfDocument.Page p=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());HazRobBranding.drawPdfHeader(p.getCanvas());return p;}')
p.write_text(s,encoding='utf-8')

# XLSX: include the controlled logo as the first embedded report image and brand the summary heading.
p=ROOT/'app/src/main/java/com/servoc/app/report/XlsxReportWriter.java';s=p.read_text(encoding='utf-8')
if 'com.servoc.app.branding.HazRobBranding' not in s:s=s.replace('import com.servoc.app.ReleaseAudit;','import com.servoc.app.ReleaseAudit;\nimport com.servoc.app.branding.HazRobBranding;')
s=s.replace('List<byte[]> charts=renderCharts(series,analytics,chemicals);','List<byte[]> charts=renderCharts(series,analytics,chemicals);charts.add(0,HazRobBranding.logoPng());')
s=s.replace('strRow(x,row++,true,"HazRob "+ReleaseAudit.VERSION,"القيمة");','strRow(x,row++,true,"HazRob SENSING • "+ReleaseAudit.VERSION,"القيمة");')
p.write_text(s,encoding='utf-8')
print('HR-0101-062 branding applied')
