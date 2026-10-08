package com.servoc.app.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Dependency-free XLSX package writer for the HR069 smart reporting engine. */
final class SmartXlsxPackage {
    private SmartXlsxPackage(){}
    static final int TEXT=0,HEADER=1,SECTION=2,WARN=3,OK=4,CRITICAL=5,NUMBER=6,PERCENT=7;

    static final class Sheet {
        final String name,xml;
        Sheet(String name,String xml){this.name=name==null?"Sheet":name;this.xml=xml==null?emptySheet():xml;}
    }

    static final class Grid {
        private final StringBuilder rows=new StringBuilder(8192);
        private final List<Double> widths=new ArrayList<>();
        private int row=1;
        Grid(double... columnWidths){if(columnWidths!=null)for(double w:columnWidths)widths.add(w);}
        void title(String text){row(new Object[]{text},new int[]{SECTION});}
        void section(String text){row(new Object[]{text},new int[]{SECTION});}
        void header(String... values){Object[] v=values==null?new Object[0]:values;int[] s=new int[v.length];for(int i=0;i<s.length;i++)s[i]=HEADER;row(v,s);}
        void row(Object... values){row(values,null);}
        void rowStyled(int style,Object... values){int[] s=new int[values==null?0:values.length];for(int i=0;i<s.length;i++)s[i]=style;row(values,s);}
        void row(Object[] values,int[] styles){if(values==null)values=new Object[0];rows.append("<row r=\"").append(row).append("\">");for(int i=0;i<values.length;i++){int st=styles!=null&&i<styles.length?styles[i]:TEXT;cell(rows,row,i+1,values[i],st);}rows.append("</row>");row++;}
        void blank(){rows.append("<row r=\"").append(row++).append("\"/>");}
        String finish(){StringBuilder x=new StringBuilder(1024+rows.length());x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");if(!widths.isEmpty()){x.append("<cols>");for(int i=0;i<widths.size();i++){double w=Math.max(7,Math.min(55,widths.get(i)));x.append("<col min=\"").append(i+1).append("\" max=\"").append(i+1).append("\" width=\"").append(w).append("\" customWidth=\"1\"/>");}x.append("</cols>");}x.append("<sheetData>").append(rows).append("</sheetData><pageMargins left=\"0.3\" right=\"0.3\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/></worksheet>");return x.toString();}
    }

    static void write(OutputStream destination,List<Sheet> sheets)throws Exception{
        if(destination==null)throw new IllegalArgumentException("output required");
        if(sheets==null||sheets.isEmpty())throw new IllegalArgumentException("sheets required");
        // Build the ZIP on disk, validate every entry, THEN copy to SAF.
        // Avoid doubling a large XLSX in memory (ByteArrayOutputStream -> toByteArray).
        File temp=File.createTempFile("hazrob-smart-report-", ".xlsx");
        try{
            try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(temp))){
                put(z,"[Content_Types].xml",contentTypes(sheets.size()));
                put(z,"_rels/.rels",rootRels());put(z,"xl/workbook.xml",workbook(sheets));
                put(z,"xl/_rels/workbook.xml.rels",workbookRels(sheets.size()));
                put(z,"xl/styles.xml",styles());
                for(int i=0;i<sheets.size();i++)put(z,"xl/worksheets/sheet"+(i+1)+".xml",sheets.get(i).xml);
                z.finish();
            }
            validate(temp,sheets.size());
            try(InputStream in=new FileInputStream(temp)){
                byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)destination.write(b,0,n);
            }
            destination.flush();
        }finally{if(!temp.delete()&&temp.exists())temp.deleteOnExit();}
    }

    private static void cell(StringBuilder x,int row,int col,Object value,int style){String ref=column(col)+row;if(value==null){x.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"/>");return;}if(value instanceof Number){double n=((Number)value).doubleValue();if(Double.isFinite(n)){x.append("<c r=\"").append(ref).append("\" s=\"").append(style==TEXT?NUMBER:style).append("\" t=\"n\"><v>").append(Double.toString(n)).append("</v></c>");return;}}if(value instanceof Boolean){x.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\" t=\"b\"><v>").append((Boolean)value?"1":"0").append("</v></c>");return;}x.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(String.valueOf(value))).append("</t></is></c>");}
    private static String column(int n){StringBuilder s=new StringBuilder();int v=Math.max(1,n);while(v>0){v--;s.insert(0,(char)('A'+v%26));v/=26;}return s.toString();}
    private static String contentTypes(int count){StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");for(int i=1;i<=count;i++)s.append("<Override PartName=\"/xl/worksheets/sheet").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");return s.append("</Types>").toString();}
    private static String rootRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";}
    private static String workbook(List<Sheet> sheets){StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><bookViews><workbookView/></bookViews><sheets>");Set<String> used=new HashSet<>();for(int i=0;i<sheets.size();i++){String n=uniqueSheetName(sheets.get(i).name,used);s.append("<sheet name=\"").append(xml(n)).append("\" sheetId=\"").append(i+1).append("\" r:id=\"rId").append(i+1).append("\"/>");}return s.append("</sheets></workbook>").toString();}
    private static String workbookRels(int count){StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");for(int i=1;i<=count;i++)s.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");return s.append("<Relationship Id=\"rId").append(count+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>").toString();}
    private static String styles(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"0.000\"/><numFmt numFmtId=\"165\" formatCode=\"0.0%\"/></numFmts><fonts count=\"3\"><font><sz val=\"10\"/><name val=\"Arial\"/></font><font><b/><sz val=\"10\"/><name val=\"Arial\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"10\"/><name val=\"Arial\"/></font></fonts><fills count=\"6\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF263238\"/><bgColor indexed=\"64\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE3F2FD\"/><bgColor indexed=\"64\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFF3E0\"/><bgColor indexed=\"64\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFEBEE\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style=\"thin\"><color rgb=\"FFCFD8DC\"/></left><right style=\"thin\"><color rgb=\"FFCFD8DC\"/></right><top style=\"thin\"><color rgb=\"FFCFD8DC\"/></top><bottom style=\"thin\"><color rgb=\"FFCFD8DC\"/></bottom><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"8\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment readingOrder=\"2\" vertical=\"top\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment horizontal=\"center\" readingOrder=\"2\" vertical=\"center\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment readingOrder=\"2\" vertical=\"center\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment readingOrder=\"2\" vertical=\"top\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment readingOrder=\"2\" vertical=\"top\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"1\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment readingOrder=\"2\" vertical=\"top\" wrapText=\"1\"/></xf><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\"><alignment readingOrder=\"2\" vertical=\"top\"/></xf><xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\"><alignment readingOrder=\"2\" vertical=\"top\"/></xf></cellXfs></styleSheet>";}
    private static String uniqueSheetName(String raw,Set<String> used){String n=raw==null?"Sheet":raw.replaceAll("[\\\\/\\?\\*\\[\\]:]"," ").trim();if(n.isEmpty())n="Sheet";if(n.length()>31)n=n.substring(0,31);String base=n;int i=2;while(used.contains(n)){String suffix=" "+i++;n=base.substring(0,Math.min(base.length(),31-suffix.length()))+suffix;}used.add(n);return n;}
    private static String xml(String s){if(s==null)return"";return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private static String emptySheet(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData/></worksheet>";}
    private static void put(ZipOutputStream z,String name,String value)throws Exception{ZipEntry e=new ZipEntry(name);z.putNextEntry(e);z.write(value.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    private static void validate(File file,int count)throws Exception{
        Set<String> names=new HashSet<>();
        try(ZipInputStream zin=new ZipInputStream(new FileInputStream(file))){
            ZipEntry e;byte[] scratch=new byte[8192];
            while((e=zin.getNextEntry())!=null){
                if(!names.add(e.getName()))throw new IllegalStateException("duplicate XLSX entry: "+e.getName());
                while(zin.read(scratch)!=-1){}
                zin.closeEntry();
            }
        }
        String[] required={"[Content_Types].xml","_rels/.rels","xl/workbook.xml","xl/_rels/workbook.xml.rels","xl/styles.xml"};
        for(String q:required)if(!names.contains(q))throw new IllegalStateException("missing XLSX entry: "+q);
        for(int i=1;i<=count;i++)if(!names.contains("xl/worksheets/sheet"+i+".xml"))throw new IllegalStateException("missing XLSX sheet "+i);
    }

}
