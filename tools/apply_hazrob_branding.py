#!/usr/bin/env python3
"""Controlled HR069-R1 branding, isolated from the scientific report/export source code.

Outputs: 1024x1024 transparent PNG for Android launch/launcher and the same
image for branded PDF/Excel renderers. Pure-Python PNG generation is deterministic.
The historical 96px WEBP output is kept only for backward build-task accounting;
it is NOT the icon used by the HR069-R1 AndroidManifest.
"""
from pathlib import Path
import base64
import sys

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from hazrob_hr069_r1_png import render_logo

OLD_B64=ROOT/'branding/hazrob_sensing_logo_96.webp.b64'
LEGACY_DRAW=ROOT/'app/src/main/res/drawable/ic_hazrob_app.webp'
PNG_DRAW=ROOT/'app/src/main/res/drawable/ic_hazrob_hr069_r1.png'
PNG_ASSET=ROOT/'app/src/main/assets/hazrob_hr069_r1_logo.png'
JAVA=ROOT/'app/src/main/java/com/servoc/app/branding/HazRobBranding.java'

for p in (LEGACY_DRAW,PNG_DRAW,PNG_ASSET,JAVA):
    p.parent.mkdir(parents=True,exist_ok=True)
LEGACY_DRAW.write_bytes(base64.b64decode(OLD_B64.read_text(encoding='ascii').strip()))
png=render_logo(1024)
PNG_DRAW.write_bytes(png)
PNG_ASSET.write_bytes(png)
logo=base64.b64encode(png).decode('ascii')
JAVA.write_text('''package com.servoc.app.branding;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

/** HR069-R1 1024px transparent high-fidelity logo. Outside-black area is alpha zero. */
public final class HazRobBranding {
 private HazRobBranding(){}
 public static final int YELLOW=0xFFFFBE00, GREY=0xFF616161, LIGHT_GREY=0xFFF5F5F5, WHITE=0xFFFFFFFF, BLACK=0xFF000000;
 private static final String LOGO_PNG="'''+logo+'''";
 public static Bitmap logo(){byte[] b=Base64.decode(LOGO_PNG,Base64.DEFAULT);return BitmapFactory.decodeByteArray(b,0,b.length);}
 public static byte[] logoPng(){byte[] b=Base64.decode(LOGO_PNG,Base64.DEFAULT);return b;}
 public static void drawPdfHeader(Canvas c){
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  p.setColor(YELLOW);c.drawRect(28,8,567,12,p);
  Bitmap b=logo();if(b!=null){c.drawBitmap(b,null,new RectF(505,14,563,72),p);b.recycle();}
 }
}
''',encoding='utf-8')

# Intentionally do not edit ReportExporter, SmartReport, XlsxReportWriter or
# any UI/transport source during preBuild. Reproducible branding is an asset step.
print('HR069-R1 branding: transparent 1024px PNG generated, bytes='+str(len(png)))
