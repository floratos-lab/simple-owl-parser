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

/* this program directly reads VO.owl and saves in the format used by HIPC signatuares dashboard */
public class VaccineDataTool {
    static final String ABOUT = "about";
    static final String CLASS = "Class";
    static final String LABEL = "label";
    static final String COMMENT = "comment";
    static final String EQUIVALENT_CLASS = "equivalentClass";

    public List<Item> readOwl(String owlFile) {
        List<Item> items = new ArrayList<Item>();
        try {
            // First, create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true); // this is important to handle &amp; correctly
            // Setup a new eventReader
            InputStream in = new FileInputStream(owlFile);
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
            // read the XML document
            Item item = null;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    // If we have an item element, we create a new item
                    if (startElement.getName().getLocalPart().equals(CLASS)) {
                        item = new Item();
                        // We read the 'about' attribute from this tag and add to our object
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();

                            // if (attribute.getName().toString().equals(DATE)) { // keep this part for reference!
                            if (attribute.getName().getLocalPart().equals(ABOUT)) {
                                String attributeValue = attribute.getValue();
                                String vaccineID = attributeValue.substring(attributeValue.lastIndexOf("/") + 1);
                                item.setVaccineID(vaccineID);
                                break;
                            }
                        }
                    } else if (startElement.getName().getLocalPart().equals(EQUIVALENT_CLASS)) { // equivalentClass element must be skipped
                        while (true) {
                            event = eventReader.nextEvent();
                            if (event.isEndElement()) {
                                EndElement endElement = event.asEndElement();
                                if (endElement.getName().getLocalPart().equals(EQUIVALENT_CLASS)) {
                                    break;
                                }
                            }
                        }
                        do {
                            event = eventReader.nextEvent();
                        } while (!event.isStartElement());
                    }

                    if (item == null)
                        continue;
                    if (item.getVaccineID() == null) { // probably inside 'subClassOf'
                        item = null;
                    }

                    if (event.asStartElement().getName().getLocalPart().equals(LABEL)) {
                        event = eventReader.nextEvent();
                        String label = event.asCharacters().getData();
                        item.setLabel(label);
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals("VO_0003099")) {
                        event = eventReader.nextEvent();
                        String tradeName = event.asCharacters().getData();
                        item.setTradeName(tradeName);
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals("VO_0003158")) {
                        event = eventReader.nextEvent();
                        String vaccineProperName = event.asCharacters().getData();
                        item.setVaccineProperName(vaccineProperName);
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals(COMMENT)) {
                        event = eventReader.nextEvent();
                        String comment = event.asCharacters().getData();
                        if (comment.startsWith("Trade Name: "))
                            item.setTradeName(comment.substring("Trade Name: ".length()));
                        continue;
                    }

                }
                // If we reach the end of an item element, we add it to the list
                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    if (endElement.getName().getLocalPart().equals(CLASS)) {
                        if (item != null && item.getLabel() != null)
                            items.add(item);
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
        String DOWNLOAD_URL = "https://raw.githubusercontent.com/vaccineontology/VO/master/src/VO_merged.owl";
        // redirected from http://purl.obolibrary.org/obo/vo.owl
        String FILENAME = "VO.owl";
        URL website = new URL(DOWNLOAD_URL);
        try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(FILENAME)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        VaccineDataTool tool = new VaccineDataTool();
        List<Item> list = tool.readOwl(FILENAME);
        System.out.println("total number " + list.size());
        try (PrintWriter pw = new PrintWriter(new FileWriter("simple-vaccine-list.txt"))) {
            pw.println("name	vaccine_id	vaccine_proper_name	trade_name");
            for (Item item : list) {
                String vaccineProperName = "";
                String tradeName = "";
                if (item.getVaccineProperName() != null)
                    vaccineProperName = item.getVaccineProperName();
                if (item.getTradeName() != null)
                    tradeName = item.getTradeName();
                pw.print(item.getLabel() + '\t' + item.getVaccineID() + '\t' + vaccineProperName + '\t' + tradeName
                        + '\n');
            }
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
