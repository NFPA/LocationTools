package org.nfpa.spatial.utils;

import com.mapzen.jpostal.AddressParser;
import com.mapzen.jpostal.ParsedComponent;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class PostalQuery implements Serializable {

    private static Map<String, Method> methodMap;
    private static AddressParser p;
    private static JSONObject abbreviations;
    private static Logger logger = Logger.getLogger(PostalQuery.class);

    public PostalQuery() {
        initLibPostal();
        getAbbreviations();
    }

    private void initLibPostal() {
        logger.info("java.library.path " + System.getProperty("java.library.path"));

        p = AddressParser.getInstance();
        methodMap = new HashMap<String, Method>();

        HashMap<String, String> libPostalMap = new HashMap<>();
        libPostalMap.put("house_number", "addHouseClause");
        libPostalMap.put("road", "addStreetClause");
        libPostalMap.put("city", "addCityClause");
        libPostalMap.put("postcode", "addZipClause");
        libPostalMap.put("state", "addStateClause");

        libPostalMap.forEach((k, v) -> {
            try {
                methodMap.put(k, PostalQuery.class.getDeclaredMethod(v, String.class, CompositeQuery.class));
            } catch (NoSuchMethodException e) {
                logger.warn(e.toString());
            }
        });
    }

    /*
    * Replace topological names with abbreviations
    * */
    private void getAbbreviations() {
        InputStream in;
        in = this.getClass().getClassLoader().getResourceAsStream("abbreviations.json");
        JSONParser jsonParser = new JSONParser();
        try {
            abbreviations = (JSONObject) jsonParser.parse(IOUtils.toString(in));
        } catch (IOException ioe){
            logger.error("IO Error reading abbreviations.json");
            logger.error(ioe);
        } catch (ParseException pe){
            logger.error("Error parsing abbreviations.json");
            logger.error(pe);
        }
    }

    private static Query addHouseClause(String houseNumber, CompositeQuery compositeQuery){
        int hno;
        try {
            hno = Integer.parseInt(houseNumber);
        }catch (NumberFormatException nfe){
            return null;
        }
        Query rAddQuery = IntRange.newContainsQuery("RADDRANGE", new int[]{hno}, new int[]{hno});
        Query lAddQuery = IntRange.newContainsQuery("LADDRANGE", new int[]{hno}, new int[]{hno});
        BooleanQuery.Builder hnoQueryBuilder = new BooleanQuery.Builder();

        hnoQueryBuilder.add(rAddQuery, BooleanClause.Occur.SHOULD);
        hnoQueryBuilder.add(lAddQuery, BooleanClause.Occur.SHOULD);
        compositeQuery.addToScore(Scores.HOUSE_NUMBER.getWeight());
        return new BoostQuery(hnoQueryBuilder.build(), Scores.HOUSE_NUMBER.getWeight()/2);
    }

    private static Query addStreetClause(String street, CompositeQuery compositeQuery){
        BooleanQuery.Builder streetQueryBuilder = new BooleanQuery.Builder();

        String[] elems = street.split("\\s+");
        if (elems.length > 1){
            SpanQuery[] clauses = new SpanQuery[elems.length];
            for (int i=0; i<clauses.length; i++){
                clauses[i] = new SpanMultiTermQueryWrapper(new FuzzyQuery(new Term("FULLNAME", elems[i]), 2));
            }
            SpanNearQuery query = new SpanNearQuery(clauses, 0
                    , true);
            streetQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
        } else {
            streetQueryBuilder.add(new FuzzyQuery(new Term("FULLNAME", street), 2), BooleanClause.Occur.SHOULD);
        }

        compositeQuery.addToScore(Scores.ROAD.getWeight());
        return new BoostQuery(streetQueryBuilder.build(), Scores.ROAD.getWeight());
    }

    private static Query addCityClause(String city, CompositeQuery compositeQuery){
        BooleanQuery.Builder cityQueryBuilder = new BooleanQuery.Builder();

        String[] elems = city.split("\\s+");
        if (elems.length > 1){
            SpanQuery[] clauses = new SpanQuery[elems.length];
            for (int i=0; i<clauses.length; i++){
                clauses[i] = new SpanMultiTermQueryWrapper(new FuzzyQuery(new Term("PLACE", elems[i]), 2));
            }
            SpanNearQuery query = new SpanNearQuery(clauses, 0
                    , true);
            cityQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
        } else {
            cityQueryBuilder.add(new FuzzyQuery(new Term("PLACE", city), 2), BooleanClause.Occur.MUST);
        }

        compositeQuery.addToScore(Scores.CITY.getWeight());
        return new BoostQuery(cityQueryBuilder.build(), Scores.CITY.getWeight());
    }

    private static Query addZipClause(String zip, CompositeQuery compositeQuery){
        Query ziplQuery = new TermQuery(new Term("ZIPL", zip));
        Query ziprQuery = new TermQuery(new Term("ZIPR", zip));
        BooleanQuery.Builder zipQueryBuilder = new BooleanQuery.Builder();

        zipQueryBuilder.add(ziplQuery, BooleanClause.Occur.SHOULD);
        zipQueryBuilder.add(ziprQuery, BooleanClause.Occur.SHOULD);

        compositeQuery.addToScore(Scores.POSTCODE.getWeight());
        return new BoostQuery(zipQueryBuilder.build(), Scores.POSTCODE.getWeight()/2);
    }

    private static Query addStateClause(String state, CompositeQuery compositeQuery){
        Query stuspsQuery = new TermQuery(new Term("STUSPS", state));
        Query stNameQuery = new TermQuery(new Term("NAME", state));
        BooleanQuery.Builder stateQueryBuilder = new BooleanQuery.Builder();

        stateQueryBuilder.add(stuspsQuery, BooleanClause.Occur.SHOULD);
        stateQueryBuilder.add(stNameQuery, BooleanClause.Occur.SHOULD);

        compositeQuery.addToScore(Scores.STATE.getWeight());
        return new BoostQuery(stateQueryBuilder.build(), Scores.STATE.getWeight());
    }

    private static String replaceWithAbbrev(String comp){
        String[] elems = comp.split("\\s+");
        String replacement;
        for (int i=0 ; i < elems.length; i++){
            replacement = (String) abbreviations.get(elems[i]);
            if (replacement != null){
                elems[i] = replacement;
            }
        }
        return String.join(" ", elems);
    }

    private static ParsedComponent[] deduplicateComponents(ParsedComponent[] parsedComponents){
        HashMap<String, String> uniqueComponents = new HashMap();

        for(ParsedComponent comp: parsedComponents){
            uniqueComponents.put(
                    comp.getLabel(),
                    uniqueComponents.get(comp.getLabel()) == null ? comp.getValue() :
//                            To concat instead of latest component, uncomment the below line
//                            uniqueComponents.get(comp.getLabel()) + " " + comp.getValue()

//                            Get the latest component
                            comp.getValue()
            );
        }

        ParsedComponent[] uniqueParsedComponents = new ParsedComponent[uniqueComponents.keySet().size()];

        int i = 0;
        for(String key :uniqueComponents.keySet()){
            uniqueParsedComponents[i] = new ParsedComponent(uniqueComponents.get(key), key);
            i++;
        }
        return uniqueParsedComponents;
    }

    public CompositeQuery makePostalQuery(String address) throws InvocationTargetException, IllegalAccessException {

        String newAddress;
        if (address == null){
            newAddress = "null";
        }else {
            newAddress = address;
        }

        ParsedComponent[] parsedComponents = p.parseAddress(newAddress);
        List<Query> queryComps = new ArrayList<>();
        BooleanQuery.Builder addressQueryBuilder = new BooleanQuery.Builder();

        CompositeQuery compositeQuery = new CompositeQuery();
        String label, value;

        for(ParsedComponent comp: deduplicateComponents(parsedComponents)){
            label = comp.getLabel();
            value = replaceWithAbbrev(comp.getValue());
            compositeQuery.addInputField("ip_postal_" + label, value);
            if (methodMap.containsKey(label)) {
                Object[] parameters = {value, compositeQuery};
                queryComps.add((Query) methodMap.get(label).invoke(null, parameters));
            }
        }
        for (Query query : queryComps){
            if (query != null){
                addressQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
            }
        }
        compositeQuery.setQuery(addressQueryBuilder.build());
        return compositeQuery;
    }
}
