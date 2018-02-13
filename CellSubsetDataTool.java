import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/* this program directly reads cell subset .owl and saves in the format used by HIPC signatuares dashboard */
public class CellSubsetDataTool {
    static final String ABOUT = "about";
    static final String CLASS = "Class";
    static final String LABEL = "label";
    static final String COMMENT = "comment";
    static final String DEFINITION = "IAO_0000115"; /* very unique tag for 'definition'! */

    static final String BROAD = "hasBroadSynonym";
    static final String EXACT = "hasExactSynonym";
    static final String RELATED = "hasRelatedSynonym";

    static final String EQUIVALENT = "equivalentClass";

    @SuppressWarnings({ "unchecked" })
    public List<CellSubet> readOwl(String owlFile) {
        List<CellSubet> items = new ArrayList<CellSubet>();
        try {
            // First, create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true); // this is important to handle &amp; correctly
            // Setup a new eventReader
            InputStream in = new FileInputStream(owlFile);
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
            // read the XML document
            CellSubet item = null;

            Set<String> broadSynonyms = null, exactSynonyms = null, relatedSynonyms = null;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    // If we have an item element, we create a new item
                    if (startElement.getName().getLocalPart().equals(CLASS)) {
                        item = new CellSubet();
                        // We read the 'about' attribute from this tag and add to our object
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();

                            // if (attribute.getName().toString().equals(DATE)) { // keep this part for reference!
                            if (attribute.getName().getLocalPart().equals(ABOUT)) {
                                String attributeValue = attribute.getValue();
                                String id = attributeValue.substring(attributeValue.lastIndexOf("/") + 1);
                                item.setId(id);
                                broadSynonyms = new HashSet<String>();
                                exactSynonyms = new HashSet<String>();
                                relatedSynonyms = new HashSet<String>();
                                break;
                            }
                        }
                    }

                    if (item == null)
                        continue;
                    if (item.getId() == null) { // probably inside 'subClassOf'
                        item = null;
                        continue;
                    }

                    /* this contains embeded Class elements, must skip */
                    if (event.asStartElement().getName().getLocalPart().equals(EQUIVALENT)) {
                        boolean ignore = true;
                        do {
                            event = eventReader.nextEvent();
                            if (event.isEndElement()
                                    && event.asEndElement().getName().getLocalPart().equals(EQUIVALENT)) {
                                ignore = false;
                            }
                        } while (ignore);
                        continue;
                    }

                    if (event.asStartElement().getName().getLocalPart().equals(LABEL)) {
                        event = eventReader.nextEvent();
                        String label = event.asCharacters().getData();
                        item.setLabel(label);
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals(COMMENT)) {
                        event = eventReader.nextEvent();
                        String comment = event.asCharacters().getData();
                        item.setComment(comment);
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals(DEFINITION)) {
                        event = eventReader.nextEvent();
                        String defnition = event.asCharacters().getData();
                        item.setDefinition(defnition);
                        continue;
                    }

                    // synonyms are a list of multiple items
                    if (event.asStartElement().getName().getLocalPart().equals(BROAD)) {
                        event = eventReader.nextEvent();
                        String broad = event.asCharacters().getData();
                        broadSynonyms.add(broad);
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals(EXACT)) {
                        event = eventReader.nextEvent();
                        String exact = event.asCharacters().getData();
                        exactSynonyms.add(exact);
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals(RELATED)) {
                        event = eventReader.nextEvent();
                        String related = event.asCharacters().getData();
                        relatedSynonyms.add(related);
                        continue;
                    }

                }
                // If we reach the end of an item element, we add it to the list
                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    if (endElement.getName().getLocalPart().equals(CLASS)) {
                        if (item != null && item.getLabel() != null) {
                            item.setBroadSynonyms(broadSynonyms);
                            item.setExactSynonyms(exactSynonyms);
                            item.setRelatedSynonyms(relatedSynonyms);
                            items.add(item);
                            broadSynonyms = new HashSet<String>();
                            exactSynonyms = new HashSet<String>();
                            relatedSynonyms = new HashSet<String>();
                        }
                    }
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return items;
    }

    static public void main(String[] agrs) throws MalformedURLException {
        String DOWNLOAD_URL = "https://raw.githubusercontent.com/obophenotype/cell-ontology/master/cl.owl";
        String FILENAME = "cl.owl";
        /*
        URL website = new URL(DOWNLOAD_URL);
        try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(FILENAME)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        CellSubsetDataTool tool = new CellSubsetDataTool();
        List<CellSubet> list = tool.readOwl(FILENAME);
        System.out.println("total number " + list.size());

        int maxDefinition = 0, maxComment = 0;
        try (PrintWriter pw = new PrintWriter(new FileWriter("cellsubset-list.txt"))) {
            pw.println("name	id	definition	comment	broad	exact	related");
            for (CellSubet item : list) {
                String broad = "";
                for (String s : item.getBroadSynonyms()) {
                    if (broad.length() == 0)
                        broad = s;
                    else
                        broad += "|" + s;
                }
                String exact = "";
                for (String s : item.getExactSynonyms()) {
                    if (exact.length() == 0)
                        exact = s;
                    else
                        exact += "|" + s;
                }
                String related = "";
                for (String s : item.getRelatedSynonyms()) {
                    if (related.length() == 0)
                        related = s;
                    else
                        related += "|" + s;
                }
                String definition = item.getDefinition();
                if (definition == null)
                    definition = "";
                else
                    definition = definition.replace("\n", " ").replace("\r", " ");
                if (definition.length() > maxDefinition)
                    maxDefinition = definition.length();

                String comment = item.getComment();
                if (comment == null)
                    comment = "";
                else
                    comment = comment.replace("\n", " ").replace("\r", " ");

                if (comment.length() > maxComment)
                    maxComment = comment.length();

                pw.print(item.getLabel() + '\t' + item.getId() + '\t' + definition + '\t' + comment + '\t' + broad
                        + '\t' + exact + '\t' + related + '\n');
            }
            pw.close();
            System.out.println("maxDefinition=" + maxDefinition + " maxComment=" + maxComment);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class CellSubet {
        private String id;
        private String label;
        private String comment;
        private String definition;

        private Set<String> broadSynonyms;
        private Set<String> exactSynonyms;
        private Set<String> relatedSynonyms;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * @return the comment
         */
        public String getComment() {
            return comment;
        }

        /**
         * @param comment the comment to set
         */
        public void setComment(String comment) {
            this.comment = comment;
        }

        /**
         * @return the definition
         */
        public String getDefinition() {
            return definition;
        }

        /**
         * @param definition the definition to set
         */
        public void setDefinition(String definition) {
            this.definition = definition;
        }

        public Set<String> getBroadSynonyms() {
            return broadSynonyms;
        }

        public void setBroadSynonyms(Set<String> broadSynonyms) {
            this.broadSynonyms = broadSynonyms;
        }

        public Set<String> getExactSynonyms() {
            return exactSynonyms;
        }

        public void setExactSynonyms(Set<String> exactSynonyms) {
            this.exactSynonyms = exactSynonyms;
        }

        public Set<String> getRelatedSynonyms() {
            return relatedSynonyms;
        }

        public void setRelatedSynonyms(Set<String> relatedSynonyms) {
            this.relatedSynonyms = relatedSynonyms;
        }

    }
}
