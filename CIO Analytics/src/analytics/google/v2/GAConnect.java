package analytics.google.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import com.google.gdata.client.analytics.AnalyticsService;
import com.google.gdata.client.analytics.DataQuery;
import com.google.gdata.data.analytics.AccountEntry;
import com.google.gdata.data.analytics.AccountFeed;
import com.google.gdata.data.analytics.DataEntry;
import com.google.gdata.data.analytics.DataFeed;
import com.google.gdata.util.AuthenticationException;

public class GAConnect {
	
    public static final String ACCOUNT_URL = "https://www.google.com/analytics/feeds/accounts/default";
    public static final String DATA_URL = "https://www.google.com/analytics/feeds/data";
    public static final int MAX_RESULTS = 1000;
    
    public static final String PROP = "analytics.";
	
	private AnalyticsService as = null;
	
	public static void main(String[] args) throws Exception {
		
		new GAConnect();
	}
	
	public GAConnect(boolean tog) throws IOException {
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream("analytics.props");
		props.load(fis);
		fis.close();
		
		accountEntries(props);
	}
	
	public GAConnect() throws Exception {
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream("analytics.props");
		props.load(fis);
		fis.close();
				
		String[] names = ((String)props.get("analytics.names")).split(",");
		
		for(String name: names) {
			System.out.println(name);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("scratch/GA/" + name+".csv")));
			
			if(name.equals("senators")) {
				Collection<SenatorObject> lst = Process.senators(this, props, name);
				printSenator(lst, bw, props.getProperty(name + ".columns"));
			}			
			else if(name.matches("viewedbills")) {
				Collection<BillObject> lst = Process.bill(this, props, name);	
				
				printBill(lst, bw, props.getProperty(name + ".columns"), props.getProperty(PROP+"enddate"));
			}
			else if(name.matches("mainsite")) {
				Collection<SourceObject> lst = Process.simpleSearch(this, props, name);
				print(lst, bw, props.getProperty(name + ".columns"), props.getProperty(PROP+"enddate"), false);
			}
			else if(name.matches("viewed\\w*|topkeyword")) {
				
				Collection<SourceObject> lst = Process.simpleSearch(this, props, name);
				print(lst, bw, props.getProperty(name + ".columns"), props.getProperty(PROP+"enddate"), true);
			}
			else if(name.matches("old")) {
				
				Collection<SourceObject> lst = Process.oldSearch(this, props, name);
								
			}
//			else if(name.matches("ja(ol|nys)r")) {
//				Process.dayRangeViewsSearch(this, props, name);
//			}
			/*if(name.equals("oldsenators")) {
				ArrayList<Integer> lVisits = new ArrayList<Integer>();
				ArrayList<Integer> lBounces = new ArrayList<Integer>();
				ArrayList<Double> lTimeOnPage = new ArrayList<Double>();
				ArrayList<Integer> lNewVisits = new ArrayList<Integer>();
				//ArrayList<Integer> lPageviews = new ArrayList<Integer>();
				
				BufferedReader br = new BufferedReader(new FileReader(new File("sen/dem")));
				
				String in = null;
				
				while((in = br.readLine()) != null) {
					String[] tuple = in.split(",");
					props.setProperty("analytics.oldsenators.id", in.split(",")[2]);
					DataQuery dq = queryBuilderFilterRegex(props,"analytics.oldsenators",props.getProperty("analytics.oldsenators.filters"));
					DataFeed df = getDataFeed(props, dq);
					
					int visits = 0;
					int bounces = 0;
					double timeOnPage = 0.0;
					int newVisits = 0;
					
					for(DataEntry de:df.getEntries()) {
						
						visits += new Integer(de.stringValueOf("ga:visits"));
						bounces += new Integer(de.stringValueOf("ga:bounces"));
						timeOnPage += new Double(de.stringValueOf("ga:timeOnPage"));
						newVisits += new Integer(de.stringValueOf("ga:newVisits"));
						System.out.println(de.stringValueOf("ga:date") + ":" + de.stringValueOf("ga:visits"));
					}
					lVisits.add(visits);
					lBounces.add(bounces);
					lTimeOnPage.add(timeOnPage);
					lNewVisits.add(newVisits);
					System.out.print(tuple[0]+",");
					break;
				}
				System.out.println();
				for(int i:lVisits) { System.out.print(i+","); }
				System.out.println();
				for(int i:lBounces) { System.out.print(i+","); }
				System.out.println();
				for(double d:lTimeOnPage) { System.out.print(d+","); }
				System.out.println();
				for(int i:lNewVisits) { System.out.print(i+","); }
				
				br.close();
			}*/
			
			bw.close();
		}	
	}
	
	public void printBill(Collection<BillObject> lst, BufferedWriter bw, String columns, String date) throws Exception {
		bw.write(columns);
		bw.newLine();
		
		for(BillObject temp:lst) {
			System.out.println(temp.getBillNo());

			String title = OpenLegConnect.get(temp.getBillNo());
			
			for(SourceObject so:temp.getSource()) {
				bw.write(date + "," 
						+ temp.getBillNo() + ",\"" 
						+ title + "\","
						+ so.getSource() + ","
						+ so.getPageviews() + ","
						+ so.getBounces() + ","
						+ so.getTime());
				bw.newLine();
			}
		}
	}
	
	public void printSenator(Collection<SenatorObject> lst, BufferedWriter bw, String columns) throws Exception {
		for(SenatorObject temp:lst) {
			bw.write(temp.getFName() + " " + temp.getLName());
			bw.newLine();
			bw.write(columns);
			bw.newLine();
			
			int bounces = 0, pageviews = 0;
			double time = 0;
			
			for(SourceObject so:temp.getSource()) {
				bw.write(so.getSource() + ","
						+ so.getPageviews() + ","
						+ so.getBounces() + ","
						+ so.getTime());
				bw.newLine();
				
				bounces += so.getBounces();
				pageviews += so.getPageviews();
				time += so.getTime();
			}
			
			bw.write("," + pageviews + "," + bounces + "," + time);
			bw.newLine();
			bw.newLine();
		}
	}
	
	public void print(Collection<SourceObject> lst, BufferedWriter bw, String columns, String date, boolean title) throws Exception {		
		bw.write(columns);
		bw.newLine();
		
		for(SourceObject so:lst) {
			bw.write(date + ","
					+ "\"" + so.getSource().replaceAll("(,|\")", " ") + "\"" + ","
					+ (title ? "\"" + OpenLegConnect.get(so.getSource()) + "\"," : "")
					+ so.getPageviews() + ","
					+ so.getBounces() + ","
					+ so.getTime());
			bw.newLine();	
			
		}

		bw.newLine();
	}
	
	private AnalyticsService getAnalyticsService(String user, String pass) {
		as = new AnalyticsService("NYSS CIO");
		try {
			as.setUserCredentials(user, pass);
		}
		catch (AuthenticationException e) {
			System.err.println("Authentication failed : " + e.getMessage());
			return null;
	    }
		return as;
	}
	
	public void accountEntries(Properties props) {
		AccountFeed af = getAccountFeed(props);
		
		for(AccountEntry ae: af.getEntries()) {
			if(ae.getTitle().getPlainText().contains("/senator/")) {
				String senName = ae.getTitle().getPlainText().split("/")[2];
				String last = senName.split("\\-")[senName.split("\\-").length-1];
				senName = senName.split("\\-")[senName.split("\\-").length-(last.equals("jr")? 2 : 1)];
				System.out.println(senName+","+ae.getTitle().getPlainText().replace("www.nysenate.gov", "") +","+ae.getProperty("ga:profileId"));
			}
			
		}		
	}
	
	public AccountFeed getAccountFeed(Properties props) {
		if(as == null) {
			as = getAnalyticsService(props.getProperty("analytics.user"),
							props.getProperty("analytics.pass"));
		}		
		try {
			URL qUrl = new URL(ACCOUNT_URL);
			
			return as.getFeed(qUrl, AccountFeed.class);
		}
		catch (Exception e) {
			return null;
		}		
	}
	
	public DataFeed getDataFeed(Properties props, DataQuery dataQuery) {
		if(as == null) {
			as = getAnalyticsService(props.getProperty("analytics.user"),
							props.getProperty("analytics.pass"));
		}		
		try {			
			return as.getFeed(dataQuery, DataFeed.class);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}		
	}
	
	public DataQuery queryBuilderFilterRegex(Properties props, String id, String filter) {
		props.setProperty(id+".filters", filter);
		return queryBuilder(props, id);
	}
	
	public DataQuery queryBuilder(Properties props, String id) {
		DataQuery dataQuery = null;
		
		String dimensions = props.getProperty(id+".dimensions");
		String metrics = props.getProperty(id+".metrics");
		String sort = props.getProperty(id+".sort");
		String filter = props.getProperty(id+".filters");
				
		try {
			URL url = new URL(DATA_URL);
			
			dataQuery = new DataQuery(url);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		dataQuery.setIds("ga:" + props.get(id+".id"));
		dataQuery.setStartDate((String)props.get("analytics.startdate"));
		dataQuery.setEndDate((String)props.get("analytics.enddate"));
		dataQuery.setMaxResults(MAX_RESULTS);
		
		if(dimensions != null)
			dataQuery.setDimensions(dimensions);
		if(metrics != null)
			dataQuery.setMetrics(metrics);
		if(sort != null)
			dataQuery.setSort(sort);
		if(filter != null)
			dataQuery.setFilters(filter);
		
		return dataQuery;
	}
	
	
	
}
