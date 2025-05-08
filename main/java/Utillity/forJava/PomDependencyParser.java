package Utillity.forJava;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;

import java.io.File;
import java.util.*;

public class PomDependencyParser {

    public static Map<String, String> parsePom(String pathToPom) {
        Map<String, String> mainDependencies = new LinkedHashMap<>();
        File pomFile = new File(pathToPom);

        if (!pomFile.exists()) {
            System.out.println("Súbor neexistuje: " + pathToPom);
            return mainDependencies;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);
            doc.getDocumentElement().normalize();

            NodeList dependencies = doc.getElementsByTagName("dependency");

            for (int i = 0; i < dependencies.getLength(); i++) {
                Node depNode = dependencies.item(i);
                if (depNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element depElement = (Element) depNode;
                    String groupId = getTextContent(depElement, "groupId");
                    String artifactId = getTextContent(depElement, "artifactId");
                    mainDependencies.put(groupId + ":" + artifactId, "(not resolved yet)");
                    //System.out.printf("- %s : %s%n", groupId, artifactId);
                }
            }

        } catch (Exception e) {
            System.out.println("Chyba pri parsovaní súboru: " + e.getMessage());
            e.printStackTrace();
        }

        return mainDependencies;
    }

    public static Map<String, String> assignVersionsFromTransitives(Map<String, String> mainDeps, List<String> transitiveLines) {
        Map<String, String> resolved = new LinkedHashMap<>();

        for (String depKey : mainDeps.keySet()) {
            for (String line : transitiveLines) {
                if (line.contains(depKey)) {
                    String[] parts = line.trim().split(":");
                    if (parts.length >= 4) {
                        resolved.put(depKey, parts[3]);
                        break;
                    }
                }
            }
            resolved.putIfAbsent(depKey, "(not found)");
        }

        return resolved;
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getParentNode().equals(parent)) {
                return node.getTextContent().trim();
            }
        }
        return "(not found)";
    }
}
