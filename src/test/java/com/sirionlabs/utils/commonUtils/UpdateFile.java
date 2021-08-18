package com.sirionlabs.utils.commonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UpdateFile {
    private final static Logger logger = LoggerFactory.getLogger(UpdateFile.class);

    public static synchronized void updateConfigFileProperty(String configFilePath, String configFileName, String propertyName, String updatedPropertyValue) {
        updateConfigFileProperty(configFilePath, configFileName, null, propertyName, null, updatedPropertyValue);
    }

    public static synchronized void updateConfigFileProperty(String configFilePath, String configFileName, String sectionName, String propertyName, String updatedPropertyValue) {
        updateConfigFileProperty(configFilePath, configFileName, sectionName, propertyName, null, updatedPropertyValue);
    }

    public static synchronized void updateConfigFileProperty(String configFilePath, String configFileName, String sectionName, String propertyName,
                                                             String subStr, String updatedPropertyValue) {
        List<String> allLines = new ArrayList<>();
        String oneLine;

        try {
            File f1 = new File(configFilePath + "/" + configFileName);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            boolean sectionFound = false;
            while ((oneLine = br.readLine()) != null) {
                if (sectionName != null) {
                    if (oneLine.trim().startsWith("[") && oneLine.contains(sectionName.trim())) {
                        sectionFound = true;
                        logger.info("Section [{}] found.", sectionName);
                    }
                } else {
                    //To handle where section is not given
                    if (!oneLine.trim().startsWith("["))
                        sectionFound = true;
                }

                if (sectionFound) {
                    allLines.add(oneLine);
                    if (sectionName == null) {
                        while (oneLine != null && !oneLine.trim().startsWith("[")) {
                            String temp[] = oneLine.trim().split(Pattern.quote("="));
                            if (temp[0].trim().equalsIgnoreCase(propertyName.trim())) {
                                if (subStr == null)
                                    allLines.add(propertyName + " = " + updatedPropertyValue);
                                else {
                                    oneLine = oneLine.replaceAll(Pattern.quote(subStr), updatedPropertyValue);
                                    allLines.add(oneLine);
                                }
                            } else {
                                allLines.add(oneLine);
                            }
                            oneLine = br.readLine();
                        }
                    } else {
                        while (((oneLine = br.readLine()) != null) && (!oneLine.trim().startsWith("["))) {
                            String temp[] = oneLine.trim().split(Pattern.quote("="));
                            if (temp[0].trim().equalsIgnoreCase(propertyName.trim())) {
                                if (subStr == null)
                                    allLines.add(propertyName + " = " + updatedPropertyValue);
                                else {
                                    oneLine = oneLine.replaceAll(Pattern.quote(subStr), updatedPropertyValue);
                                    allLines.add(oneLine);
                                }
                            } else {
                                allLines.add(oneLine);
                            }
                        }
                    }

                    if (oneLine != null)
                        allLines.add(oneLine);

                    while ((oneLine = br.readLine()) != null)
                        allLines.add(oneLine);
                    break;
                }
                allLines.add(oneLine);
            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            for (int i = 0; i < allLines.size() - 1; i++) {
                out.write(allLines.get(i) + "\n");
            }
            out.write(allLines.get(allLines.size() - 1));

            out.flush();
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Updating File. {}", e.getMessage());
        }
    }


    public static synchronized void addPropertyToConfigFile(String configFilePath, String configFileName, String sectionName, String propertyName,
                                                            String value) {
        List<String> allLines = new ArrayList<>();
        String oneLine;

        try {
            File f1 = new File(configFilePath + "/" + configFileName);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            boolean sectionFound = false;
            while ((oneLine = br.readLine()) != null) {
                if (sectionName != null) {
                    if (oneLine.trim().startsWith("[") && oneLine.contains(sectionName.trim())) {
                        sectionFound = true;
                        logger.info("Section [{}] found.", sectionName);
                    }
                } else {
                    //To handle where section is not given
                    if (!oneLine.trim().startsWith("["))
                        sectionFound = true;
                }

                if (sectionFound) {
                    allLines.add(oneLine);
                    String propertyToBeAdded = propertyName + " = " + value;
                    allLines.add(propertyToBeAdded);
                    if (sectionName == null) {
                        while (oneLine != null && !oneLine.trim().startsWith("[")) {
                            allLines.add(oneLine);
                            oneLine = br.readLine();
                        }
                    } else {
                        while (((oneLine = br.readLine()) != null) && (!oneLine.trim().startsWith("["))) {
                            allLines.add(oneLine);
                        }
                    }

                    if (oneLine != null)
                        allLines.add(oneLine);

                    while ((oneLine = br.readLine()) != null)
                        allLines.add(oneLine);

                    break;

                }
                allLines.add(oneLine);
            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            for (int i = 0; i < allLines.size() - 1; i++) {
                out.write(allLines.get(i) + "\n");
            }
            out.write(allLines.get(allLines.size() - 1));

            out.flush();
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Updating File. {}", e.getMessage());
        }
    }


    public static synchronized void addPropertiesToConfigFile(String configFilePath, String configFileName, String sectionName, Map<String, String> map) {
        List<String> allLines = new ArrayList<>();
        String oneLine;

        try {
            File f1 = new File(configFilePath + "/" + configFileName);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            boolean sectionFound = false;
            while ((oneLine = br.readLine()) != null) {
                if (sectionName != null) {
                    if (oneLine.trim().startsWith("[") && oneLine.contains(sectionName.trim())) {
                        sectionFound = true;
                        logger.info("Section [{}] found.", sectionName);
                    }
                } else {
                    //To handle where section is not given
                    if (!oneLine.trim().startsWith("["))
                        sectionFound = true;
                }

                if (sectionFound) {
                    allLines.add(oneLine);
                    for (Map.Entry<String, String> entry : map.entrySet())
                        allLines.add(entry.getKey() + "=" + entry.getValue());
                    if (sectionName == null) {
                        while (oneLine != null && !oneLine.trim().startsWith("[")) {
                            allLines.add(oneLine);
                            oneLine = br.readLine();
                        }
                    } else {
                        while (((oneLine = br.readLine()) != null) && (!oneLine.trim().startsWith("["))) {
                            allLines.add(oneLine);
                        }
                    }

                    if (oneLine != null)
                        allLines.add(oneLine);

                    while ((oneLine = br.readLine()) != null)
                        allLines.add(oneLine);

                    break;

                }
                allLines.add(oneLine);
            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            for (int i = 0; i < allLines.size() - 1; i++) {
                out.write(allLines.get(i) + "\n");
            }
            out.write(allLines.get(allLines.size() - 1));

            out.flush();
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Updating File. {}", e.getMessage());
        }
    }

    public static synchronized void deletePropertyFromConfigFile(String configFilePath, String configFileName, String sectionName, String propertyName) {
        List<String> allLines = new ArrayList<>();
        String oneLine;

        try {
            File f1 = new File(configFilePath + "/" + configFileName);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            boolean sectionFound = false;
            while ((oneLine = br.readLine()) != null) {
                if (sectionName != null) {
                    if (oneLine.trim().startsWith("[") && oneLine.contains(sectionName.trim())) {
                        sectionFound = true;
                        logger.info("Section [{}] found.", sectionName);
                    }
                } else {
                    //To handle where section is not given
                    if (!oneLine.trim().startsWith("["))
                        sectionFound = true;
                }

                if (sectionFound) {
                    allLines.add(oneLine);
                    if (sectionName == null) {
                        while (oneLine != null && !oneLine.trim().startsWith("[")) {
                            if (!oneLine.startsWith(propertyName))
                                allLines.add(oneLine);
                            oneLine = br.readLine();
                        }
                    } else {
                        while (((oneLine = br.readLine()) != null) && (!oneLine.trim().startsWith("["))) {
                            if (!oneLine.startsWith(propertyName)) {
                                allLines.add(oneLine);
                            }
                        }
                    }

                    if (oneLine != null) {
                        if (!oneLine.startsWith(propertyName))
                            allLines.add(oneLine);
                    }

                    while ((oneLine = br.readLine()) != null)
                        if (!oneLine.startsWith(propertyName))
                            allLines.add(oneLine);

                    break;

                }
                allLines.add(oneLine);
            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            for (int i = 0; i < allLines.size() - 1; i++) {
                out.write(allLines.get(i) + "\n");
            }
            out.write(allLines.get(allLines.size() - 1));

            out.flush();
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Updating File. {}", e.getMessage());
        }
    }
}
