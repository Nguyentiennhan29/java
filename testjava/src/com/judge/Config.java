package com.judge;

import java.io.*;
import java.util.Properties;

public class Config {
    private static Config instance;
    private final Properties props = new Properties();
    private static final String FILE = System.getProperty("user.home") + "/.cj_config.properties";

    private Config() {
        File f = new File(FILE);
        if (f.exists()) try (InputStream is = new FileInputStream(f)) { props.load(is); }
        catch (IOException ignored) {}
    }

    public static synchronized Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }

    public void save() {
        try (OutputStream os = new FileOutputStream(FILE)) { props.store(os, "CJ Config"); }
        catch (IOException ignored) {}
    }

    public String getApiKey()              { return props.getProperty("api_key", ""); }
    public void   setApiKey(String v)      { props.setProperty("api_key", v); }
    public String getModel()               { return props.getProperty("model", "gemini-2.0-flash"); }
    public void   setModel(String v)       { props.setProperty("model", v); }
    public int    getTimeout()             { return Integer.parseInt(props.getProperty("timeout", "5000")); }
    public void   setTimeout(int v)        { props.setProperty("timeout", String.valueOf(v)); }
    public String getGppPath()             { return props.getProperty("gpp", "g++"); }
    public void   setGppPath(String v)     { props.setProperty("gpp", v); }
    public String getPythonPath()          { return props.getProperty("python", "python3"); }
    public void   setPythonPath(String v)  { props.setProperty("python", v); }
    public String getLanguage()            { return props.getProperty("language", "C++"); }
    public void   setLanguage(String v)    { props.setProperty("language", v); }
}