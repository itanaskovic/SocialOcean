package utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.spatial.geopoint.search.GeoPointInBBoxQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import interfaces.ILuceneQuerySearcher;

// as singleton
public enum Lucene {
	INSTANCE;
	
	// Hard Coded Parameters
	private  String field = "content";		// standard field
	private  String geoField = "geo";		// standard geoField
	private  Resolution dateResolution = Resolution.MILLISECOND;
	
	private  IndexReader reader;
	private  IndexSearcher searcher;
	private  Analyzer analyzer;
	private  QueryParser parser;
	private  ILuceneQuerySearcher querySearcher;
	
	private  IndexInfo idxInfo;
	private  int numTerms = 0;
	private  Map<String, FieldTermCount> termCounts;
	
	private  ArrayList<String> queryStrings = new ArrayList<>();
	private  ArrayList<QueryResult> queryResults = new ArrayList<>();
	
	private List<String> fn;
	private String[] idxFields = null;
	
//	public static void main(String[] args) {
//		String formattedString = String.format("%s \t\t %.2f \t %d %s", "Test", 1*100/(float)4, 15, "%");       
//		System.out.println(formattedString);
//	}
	
	public  int serialCounter = 0;
	public boolean isInitialized = false;
	
	public void initLucene(String index, ILuceneQuerySearcher querySearcher) {
		try {
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
			idxInfo = new IndexInfo(reader, index);
			fn = idxInfo.getFieldNames();
			numTerms = idxInfo.getNumTerms();
            termCounts = idxInfo.getFieldTermCounts();
            idxFields = fn.toArray(new String[fn.size()]);
			if (fn.size() == 0) {
				System.out.println("Empty index.");
				// showStatus("Empty index."); --> print on Console ..
			}
		} catch (IOException e) {
			System.out.println("Could not create LuceneSearcher, path to index not found "+index);
			e.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		searcher = new IndexSearcher(reader);
		analyzer = new StandardAnalyzer();
		parser = new QueryParser(field, analyzer);
		parser.setDateResolution(dateResolution);
		this.querySearcher = querySearcher;
		querySearcher.initQuerySearcher(searcher, analyzer);
		isInitialized = true;
		
		printStatistics();
		
	}
	
	private void printStatistics() {
		
		System.out.println("Index name: \t"+idxInfo.getIndexPath());
		System.out.println("Lucene Version: \t"+idxInfo.getVersion());
		System.out.println("Number of fieds: \t"+termCounts.size());
		System.out.println("Number of terms: \t"+numTerms);
		System.out.println("Filesize of Index: \t"+idxInfo.getTotalFileSize()/1000000+" MB");
		
		// sort by names now
		String[] idxFieldsCopy = idxFields.clone();

		// sort by term count
		ValueComparator bvc = new ValueComparator(termCounts);
		TreeMap<String, FieldTermCount> termCountsSorted = new TreeMap<String, FieldTermCount>(bvc);
		termCountsSorted.putAll(termCounts);
//		String[] sortedFields = termCountsSorted.keySet().toArray(new String[termCounts.size()]);
//		String[] idxFieldsCopySorted = new String[idxFields.length];
//		System.arraycopy(sortedFields, 0, idxFieldsCopySorted, 0, sortedFields.length);
//		if (termCounts.size() < idxFieldsCopy.length) {
//			int idx = sortedFields.length;
//			for (String f : idxFields) {
//				if (!termCounts.containsKey(f)) {
//					idxFieldsCopySorted[idx] = f;
//					idx += 1;
//				}
//			}
//		}
		System.out.println(String.format("\n%-10s %15s \t %-10s", "Name", "Term Count", "%"));
		for (FieldTermCount ftc : termCountsSorted.values()) {
			String formattedString = String.format("%-10s %15d \t %.2f %s" ,ftc.fieldname, ftc.termCount,(ftc.termCount*100)/(float)numTerms, "%");       
			System.out.println(formattedString); 
		}
	}
	

	public void changeAnalyser(Analyzer newAnalyser) {
		analyzer = newAnalyser;
		querySearcher.changeAnalyzer(analyzer);
	}
	
	public ILuceneQuerySearcher getQuerySearch() {
		return querySearcher;
	}
	
	public ArrayList<QueryResult> getQueries() {
		return queryResults;
	}
	
	public void resetQueryHistory() {
		serialCounter = 0;
		queryStrings.clear();
		queryResults.clear();
	}
	
	public void deleteAtIndex(int index) {
		if (index >= queryStrings.size())
			return;
		queryStrings.remove(index);
		queryResults.remove(index);
	}
	
	public void ADDQuery(Query query) {
		serialCounter++;
		queryStrings.add(query.toString());
		ScoreDoc[] result = querySearcher.searchAll(query);
		QueryResult qr = new QueryResult(query, result, serialCounter);
		queryResults.add(qr);
		
		System.out.println(qr.toString());
	}
	
	public void FUSEQuery(int index, Query newQuery) {
	
		// fuse query this index - delete index, add new, inc serial
		serialCounter++;
		ScoreDoc[] result = querySearcher.fuseQuery(queryResults.get(index).query, newQuery);
		
	}
	
	public void FUSEQueries(ArrayList<Integer> queryIndexes) {
		
		// fuse queries from the query result array
		// delete the indexes - add a new, inc serial
	}
	
	/**
	 * This method searches for the <code>topX</code> documents on a single lucene <code>field</code>
	 * @param field
	 * @param topX
	 * @throws IOException 
	 */
	public void searchTopXOfField(String field, int topX)  {
		String[] fields = {field};
		try {
			// TODO put into external thread!
			TermStats[] result = HighFreqTerms.getHighFreqTerms(reader, topX, fields);
			for (TermStats ts : result) {
				System.out.println(ts.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void ADDGeoQuery(double minLat, double maxLat, double minLong, double maxLong) {
		Query query = new GeoPointInBBoxQuery(geoField, minLat, maxLat, minLong, maxLong);
		ADDQuery(query);
	}
	
	public void searchTimeRange(long from, long to) {
		try {
			Query query = parser.parse("date:["+from +" TO "+to +"]");
			ADDQuery(query);
		} catch (ParseException e) {
			System.out.println("Could not Parse Date Search to Query");
			e.printStackTrace();
		}
	}
	
	
	
	
	public QueryParser getParser() {
		return parser;
	}
	
	
	private class ValueComparator implements Comparator<String> {
		Map<String, FieldTermCount> base;

		public ValueComparator(Map<String, FieldTermCount> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with
		// equals.
		public int compare(String a, String b) {
			return base.get(a).compareToValues(base.get(b));
		}
	}
	
		
	public class QueryResult implements Comparable<QueryResult>{
		
		private int maxChars = 20;
		private int serial;
		private Query query;
		private ScoreDoc[] result;
		
		public QueryResult(Query query, ScoreDoc[] result, int serial ) {
			this.serial = serial;
			this.query = query;
			this.result = result;
		}

		@Override
		public int compareTo(QueryResult qr) {
			return Integer.compare(serial, qr.serial);
		}
		
		
		// for queryHistory
		public String shortString() {
			return "("+ serial +") "+query.toString().substring(0, maxChars);
		}
		
		// for a tooltip
		@Override
		public String toString() {
			return "("+serial+") "+query.toString()+" #:"+result.length;
		}
		
	}
	
}