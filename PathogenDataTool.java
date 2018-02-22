import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
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

/* this program directly reads pathogen .owl and saves in the format used by HIPC signatuares dashboard */
public class PathogenDataTool {
    static final String ABOUT = "about";
    static final String CLASS = "Class";
    static final String LABEL = "label";
    static final String RANK = "has_rank";

    static final String BROAD = "hasBroadSynonym";
    static final String EXACT = "hasExactSynonym";
    static final String RELATED = "hasRelatedSynonym";

    static final String EQUIVALENT = "equivalentClass";

    @SuppressWarnings({ "unchecked" })
    public List<Pathogen> readOwl(String owlFile) {
        List<Pathogen> items = new ArrayList<Pathogen>();
        try {
            // First, create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true); // this is important to handle &amp; correctly
            // Setup a new eventReader
            InputStream in = new FileInputStream(owlFile);
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
            // read the XML document
            Pathogen item = null;

            Set<String> broadSynonyms = null, exactSynonyms = null, relatedSynonyms = null;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    // If we have an item element, we create a new item
                    if (startElement.getName().getLocalPart().equals(CLASS)) {
                        item = new Pathogen();
                        // We read the 'about' attribute from this tag and add to our object
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();

                            // if (attribute.getName().toString().equals(DATE)) { // keep this part for reference!
                            if (attribute.getName().getLocalPart().equals(ABOUT)) {
                                String attributeValue = attribute.getValue();
                                String id = attributeValue.substring(attributeValue.lastIndexOf("/") + 1);
                                id = id.substring("NCBITaxon_".length());
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
                    if (event.asStartElement().getName().getLocalPart().equals(RANK)) {
                        StartElement e = event.asStartElement();
                        Iterator<Attribute> attributes = e.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();

                            if (attribute.getName().getLocalPart().equals("resource")) {
                                String attributeValue = attribute.getValue();
                                String rank = attributeValue.substring(attributeValue.lastIndexOf("/") + 1);
                                rank = rank.substring("NCBITaxon_".length());
                                item.setRank(rank);
                                break;
                            }
                        }
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
                        if (item != null && item.getLabel() != null && !item.getLabel().equals("root")) {
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

    public void testReadOwl(String owlFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(owlFile))) {
            int i = 0;
            String line = br.readLine();
            while (i < 1200 && line != null) {
                System.out.println(line);
                line = br.readLine();
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void main(String[] agrs) throws MalformedURLException {
        String DOWNLOAD_URL = "http://purl.obolibrary.org/obo/ncbitaxon.owl";
        String FILENAME = "ncbitaxon.owl";

        /*
        URL website = new URL(DOWNLOAD_URL);
        try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(FILENAME)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        PathogenDataTool tool = new PathogenDataTool();
        //tool.testReadOwl(FILENAME);
        List<Pathogen> list = tool.readOwl(FILENAME);
        System.out.println("total number " + list.size());

        try (PrintWriter pw = new PrintWriter(new FileWriter("pathogen-list.txt"))) {
            pw.println("name	id	rank	broad	exact	related");
            for (Pathogen item : list) {
                pw.print(item);
            }
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Pathogen {
        private String id;
        private String label;
        private String rank;

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

        public String getRank() {
            return rank;
        }

        public void setRank(String rank) {
            this.rank = rank;
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

        @Override
        public String toString() {
            String broad = "";
            for (String s : getBroadSynonyms()) {
                if (broad.length() == 0)
                    broad = s;
                else
                    broad += "|" + s;
            }
            String exact = "";
            for (String s : getExactSynonyms()) {
                if (exact.length() == 0)
                    exact = s;
                else
                    exact += "|" + s;
            }
            String related = "";
            for (String s : getRelatedSynonyms()) {
                if (related.length() == 0)
                    related = s;
                else
                    related += "|" + s;
            }
            String rank = getRank();
            if (rank == null)
                rank = "";
            else
                rank = rank.replace("\n", " ").replace("\r", " ");

            return (getLabel() + '\t' + getId() + '\t' + rank + '\t' + broad + '\t' + exact + '\t' + related
                    + '\n');
        }
    }
}
