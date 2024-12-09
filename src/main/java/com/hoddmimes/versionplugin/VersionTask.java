package com.hoddmimes.versionplugin;

import com.google.gson.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionTask extends DefaultTask {
    private static final String DEFAULT_ACTION = "generate";
    @Input
    static String versionFile; // Json version file location

    @Input
    static String generatedVersionFile; // Generated Java version file location

    @Input
    String action = DEFAULT_ACTION; // Action (bump)  'minor' or 'major' or 'generate'
    @TaskAction
    public void action() {
        JsonObject jVersion = readVersionFile();

        if (!jVersion.has("releasenotes")) {
            jVersion.addProperty("realeasenotes", "null" );
        }

        if (action.toLowerCase().contentEquals("minor")) {
            int tMinor = jVersion.get("minor").getAsInt() + 1;
            int tMajor = jVersion.get("major").getAsInt();
            createVersionFile( tMajor, tMinor, "null" );
            jVersion.addProperty("minor", tMinor );
            System.out.println("***** Bumping Minor");
        } else if (action.toLowerCase().contentEquals("minor")) {
            int tMajor = jVersion.get("major").getAsInt() + 1;
            jVersion.addProperty("minor", 0 );
            jVersion.addProperty("major", tMajor );
            createVersionFile( tMajor, 0, "null" );
            System.out.println("***** Bumping Major");
        }

        // Always generate a Java version file
        String tReleaseNotesFilename = (jVersion.has("releaseNotes")) ? jVersion.get("releaseNotes").getAsString() : null;
        generateJavaVersionFile(jVersion.get("major").getAsInt(), jVersion.get("minor").getAsInt(), tReleaseNotesFilename);
    }

    private void generateJavaVersionFile(int pMajor, int pMinor, String pReleaseNotesFileName ) {
        // "src/main/java/com/hoddmimes/plugintest/generated/foobar.java"
        Pattern tPackagePattern = Pattern.compile(".*/(com.*)/.*\\.java");
        Matcher m = tPackagePattern.matcher( generatedVersionFile );
        if (!m.find()) {
            throw new RuntimeException("Can not find package name in version out file");
        }
        String tPackage = m.group(1).replace("/",".");
       // System.out.println("Major: " + pMajor + " Minor: " + pMinor + " ReleaseFile: " + pReleaseNotesFileName +
        //        " Generated version file: " + generatedVersionFile  + " ProjDir: " + getProjectDir());
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss");
            String tBuildTime = sdf.format(System.currentTimeMillis());
            PrintWriter pw = new PrintWriter(new PrintWriter(new File(generatedVersionFile)));

            pw.println("package " + tPackage + ";\n\n");
            pw.println("import java.util.List;");
            pw.println("import java.util.ArrayList;");
            pw.println("/*");
            pw.println("  ====================================================================================");
            pw.println(" * Note: This file is automatically generated as part of the build process (i.e. build.gradle)");
            pw.println(" * Do not change or edit this file");
            pw.println("  =====================================================================================");
            pw.println("*/\n\n");
            pw.println("public class Version");
            pw.println("{");
            pw.println("    public static final String buildDate = \"" + tBuildTime + "\";");
            pw.println("    public static final int major = " + pMajor + ";");
            pw.println("    public static final int minor = " + pMinor + ";");
            pw.println("    public static final String build = \"version: " + pMajor + "." + pMinor + " built: " + tBuildTime + "\";");
            if (pReleaseNotesFileName != null) {

                File tFile = new File( getProjectDir() + "/" +pReleaseNotesFileName);
                if ((!tFile.exists()) || (!tFile.canRead())) {
                    System.out.println("Could not find release notes file: " + (getProjectDir() + "/" + pReleaseNotesFileName));
                }
                if (tFile.canRead()) {
                    List<String> tLines = Files.readAllLines(Paths.get(pReleaseNotesFileName));
                    StringBuilder sb = new StringBuilder();
                    pw.println("    public static final List<String> release() {");
                    pw.println("      List<String> tList = new ArrayList();");
                    for (String line : tLines) {
                        pw.println("       tList.add(\"" + line + "\");");
                    }
                    pw.println("      return tList;");
                    pw.println("    }");

                    pw.println("    public static final String releaseText = ");
                    for (String line : tLines) {
                        pw.println("        \"" + line + "\\n\" +");
                    }
                    pw.println("        \"\\n\" ;");
                }
            }
            pw.println("}\n");
            pw.flush();
            pw.close();
        }
        catch( IOException e) {
            throw new RuntimeException(e);
        }
    }


    private String getProjectDir() {
        return getProject().getProjectDir().getAbsolutePath();
    }

    private void createJsonVersionFileIfNotExits() {
        File tFile = new File(  versionFile );
        if (tFile.canRead()) {
            return;
        }
        createVersionFile( 1, 0, null );
    }

    private JsonObject readVersionFile() {
        createJsonVersionFileIfNotExits();
        try {
            JsonObject jVersion = JsonParser.parseReader( new FileReader(versionFile)).getAsJsonObject();
            return jVersion;
        }
        catch( IOException e) {
          throw new RuntimeException(e);
        }
    }

    private void createVersionFile( int pMajor, int pMinor, String pReleaseNotes ) {
        JsonObject jVersion = new JsonObject();
        jVersion.addProperty("major", pMajor );
        jVersion.addProperty("minor", pMinor );
        jVersion.addProperty("releasenotes", pReleaseNotes );
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File tFile = new File( versionFile );
        try {
            PrintWriter pw = new PrintWriter(tFile, Charset.defaultCharset());
            pw.print( gson.toJson(jVersion));
            pw.close();
        }
        catch( IOException e) {
            throw new RuntimeException( e );
        }
    }

    public void setAction( String pAction ) {
        this.action = pAction;
    }

    public String getAction() {
        return this.action;
    }

    public void setVersionFile( String pVersionFile ) {
        this.versionFile = pVersionFile;
    }

    public String getVersionFile() {
        return this.versionFile;
    }

    public void setGeneratedVersionFile( String pVersionFile ) {
        this.generatedVersionFile = pVersionFile;
    }

    public String getGeneratedVersionFile() {
        return this.generatedVersionFile;
    }

}
