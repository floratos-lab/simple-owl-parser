import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/* 
while there is an available OWL parser, 
see http://owlcs.github.io/owlapi/ and
http://repo1.maven.org/maven2/net/sourceforge/owlapi/owlapi-compatibility/5.1.4/ and
http://owlapi.sourceforge.net/owled2011_tutorial.pdf
it is an overkill for the current need of parsing very few fields, 
and at same time does not handle the comment element as needed
*/

// this program basically follows the example from http://www.vogella.com/tutorials/JavaXML/article.html
public class SimpleOWLParser {
    static final String ABOUT = "about";
    static final String CLASS = "Class";
    static final String LABEL = "label";
    static final String COMMENT = "comment";

    @SuppressWarnings({ "unchecked" })
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
                                String vaccineID = attributeValue.substring(attributeValue.lastIndexOf("/")+1);
                                item.setVaccineID(vaccineID);
                                break;
                            }
                        }
                    }

                    if(item==null) continue;
                    if(item.getVaccineID()==null) { // probaly inside 'subClassOf'
                        item = null;
                    }

                    if (event.asStartElement().getName().getLocalPart()
                                .equals(LABEL)) {
                            event = eventReader.nextEvent();
                            String label = event.asCharacters().getData();
                            item.setLabel(label);
                            continue;
                    }
                    if (event.asStartElement().getName().getLocalPart()
                            .equals(COMMENT)) {
                        event = eventReader.nextEvent();
                        String comment = event.asCharacters().getData();
                        // only 64 Prodct Name and 106 Trade Name
                        if(comment.startsWith("Product Name: "))
                            item.setProductName(comment.substring("Product Name: ".length()));
                        if(comment.startsWith("Trade Name: "))
                            item.setTradeName(comment.substring("Trade Name: ".length()));
                        continue;
                    }

                }
                // If we reach the end of an item element, we add it to the list
                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    if (endElement.getName().getLocalPart().equals(CLASS)) {
                        if(item!=null)
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

    static public void main(String[] agrs) {
        SimpleOWLParser read = new SimpleOWLParser();
        List<Item> list = read.readOwl("VO.owl");
        for (Item item : list) {
            System.out.println(item);
        }
        System.out.println("total number "+list.size());
    }
}
