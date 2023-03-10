package com.hoddmimes.versionplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
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

        if (action.toLowerCase().contentEquals("minor")) {
            int tMinor = jVersion.get("minor").getAsInt() + 1;
            int tMajor = jVersion.get("major").getAsInt();
            createVersionFile( tMajor, tMinor );
            jVersion.addProperty("minor", tMinor );
            System.out.println("***** Bumping Minor");
        } else if (action.toLowerCase().contentEquals("minor")) {
            int tMajor = jVersion.get("major").getAsInt() + 1;
            jVersion.addProperty("minor", 0 );
            jVersion.addProperty("major", tMajor );
            createVersionFile( tMajor, 0 );
            System.out.println("***** Bumping Major");
        }

        // Always generate a Java version file
        generateJavaVersionFile(jVersion.get("major").getAsInt(), jVersion.get("minor").getAsInt());
    }

    private void generateJavaVersionFile(int pMajor, int pMinor ) {
        // "src/main/java/com/hoddmimes/plugintest/gnerated/foobar.java"
        Pattern tPackagePattern = Pattern.compile(".*/(com.*)/.*\\.java");
        Matcher m = tPackagePattern.matcher( generatedVersionFile );
        if (!m.find()) {
            throw new RuntimeException("Can not find package name in version out file");
        }
        String tPackage = m.group(1).replace("/",".");

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss" );
            String tBuildTime = sdf.format( System.currentTimeMillis());
            PrintWriter pw = new PrintWriter( new PrintWriter( new File( generatedVersionFile)));

            pw.println("package " + tPackage +";\n\n");
            pw.println("/*");
            pw.println("  ====================================================================================");
            pw.println(" * Note: This file is automatically generated as part of the build process (i.e. build.gradle)");
            pw.println(" * Do not change or edit this file");
            pw.println("  =====================================================================================");
            pw.println("*/\n\n");
            pw.println("public class Version");
            pw.println("{");
            pw.println("    public static final String buildDate = \"" + tBuildTime + "\";");
            pw.println("    public static final int major = " + pMajor + ";" );
            pw.println("    public static final int minor = " + pMinor + ";" );
            pw.println("    public static final String build = \"version: " + pMajor + "." + pMinor + " built: " + tBuildTime + "\";");
            pw.println("}\n");
            pw.flush();
            pw.close();
        }
        catch( IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void createJsonVersionFileIfNotExits() {
        File tFile = new File(  versionFile );
        if (tFile.canRead()) {
            return;
        }
        createVersionFile( 1, 0 );
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

    private void createVersionFile( int pMajor, int pMinor ) {
        JsonObject jVersion = new JsonObject();
        jVersion.addProperty("major", pMajor );
        jVersion.addProperty("minor", pMinor );
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
