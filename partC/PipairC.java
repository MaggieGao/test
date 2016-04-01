
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;

public class PipairC {
	
	/* store function(s) called by it(their) caller(s) as value in HashMap pipair, 
	 * use HashSet to eliminate duplicate functions */
	public static Set<String> stringset = new HashSet<String>();
	
	/* record function(s) and it(their) caller(s) as key-value set in Map*/
	public static Map<HashSet<String>, HashSet<String>> pipair = new HashMap<HashSet<String>, HashSet<String>>();
	
	public static void main(String [] args) throws Exception{
		
		int support = 3;
		float confidence = (float)0.65;
		int expansion=1;
		
		switch(args.length){
			//case 0: break;
			case 1: break;
			case 3:
				support = Integer.parseInt(args[1]);
				confidence = Float.parseFloat(args[2])/100;
				break;
			case 4: 
				support = Integer.parseInt(args[1]);
				confidence = Float.parseFloat(args[2])/100;
				expansion= Integer.parseInt(args[3]);
				break;
			default:
				System.err.println("Error: Wrong arguments input.");
                System.exit(1);
		}
		
		/* read callgraph file into program and store in map */
		readFile(expansion);
		findBug(support, confidence);
		
		return;
	}

	static void readFile(int expansion) throws IOException{
		
		HashMap<String,HashSet<String>>  caller_to_callee=new HashMap<String,HashSet<String>>(); //save all caller_callee relationship
		String caller;
		
		//File file = new File("src/q1/graphoutput.txt");
		//Scanner scanner = new Scanner(file);
		
		Scanner scanner = new Scanner(System.in);
		
		String pattern_caller = "Call.*'(.*)'.*";
		Pattern p_caller = Pattern.compile(pattern_caller);
		Matcher m_caller = null;
		
		String pattern_callee = ".*calls.*'(.*)'.*";
		Pattern p_callee = Pattern.compile(pattern_callee);
		Matcher m_callee = null;
		
		String line = null; 
		String add_value = null;
		String add_key = null;

		while(scanner.hasNext()){
			HashSet<String> callee_temp=new HashSet<String>();  //must do duplicate set instantiation before map.put every time
			line = scanner.nextLine();
			m_caller = p_caller.matcher(line);
			if(m_caller.matches()){
				add_value = m_caller.group(1);
				stringset.clear();
				while(scanner.hasNext()){
					line = scanner.nextLine();
					if(line.length() == 0){
						break;
					}
					
					m_callee = p_callee.matcher(line);
					if(m_callee.matches()){
						add_key = m_callee.group(1);
						callee_temp.add(add_key);     //save callee to temp
						if(!stringset.contains(add_key)){
							addToMap(add_key, add_value);
							stringset.add(add_key);
						}
					}
				}

				caller_to_callee.put(add_value, callee_temp); //save caller-[callee] pairs
				//System.out.println(caller_to_callee);
			}
		}
		//System.out.println(pipair);	
		scanner.close();

		//loop through all caller-callee pairs to the expansion level
		HashSet<String> callee_temp1=new HashSet<String>();
		for(int i=0;i<expansion;i++){
			HashMap<String,HashSet<String>>  caller_to_callee_temp=new HashMap<String,HashSet<String>>();
			//go through all functions and expand them
			for(Map.Entry<String, HashSet<String>> entry:caller_to_callee.entrySet()){
				caller=entry.getKey();
				callee_temp1=entry.getValue();
				HashSet<String> callee_temp2=new HashSet<String>(); 
				//expand 1 function and store the expanded funtion to temp
				for(String callee:callee_temp1){
					callee_temp2.addAll(caller_to_callee.get(callee));
				}
				caller_to_callee_temp.put(caller, callee_temp2);
				stringset=callee_temp2;
				//add all expanded function to hashmap
				for(String callee2:callee_temp2){
					addToMap(callee2,caller);
				}
			}
			caller_to_callee=caller_to_callee_temp; //update hashmap for next level of expansion
		}
		return;
	}
	
	static void addToMap(String add_key, String add_value){
		
		HashSet<String> map_value = null;
		HashSet<String> map_key = new HashSet<String>();
		
 		/* map_key only has one element */
 		map_key.add(add_key);
 		if(pipair.containsKey(map_key)){
			map_value = pipair.get(map_key);
		} else{
			map_value = new HashSet<String>();
		}
		map_value.add(add_value);
		
		pipair.put(map_key, map_value);
		
		/* map_key has two elements */
		for(String key:stringset){

			map_key = new HashSet<String>();
			map_key.add(add_key);
			map_key.add(key);
			if(pipair.containsKey(map_key)){
				map_value = pipair.get(map_key);
			} else{
				map_value = new HashSet<String>();
			}
			map_value.add(add_value);
			
			pipair.put(map_key, map_value);
		}
		
		return;
	}
	
	static void findBug(int support, float confidence){
		/*  two HashMap separate pipairs whose keys include one element and two, 
		 *  key is the same as that in pipair,
		 *  value is the number of elements of the corresponding value in pipair */
		HashMap<HashSet<String>, Integer> pipair_single = new HashMap<HashSet<String>, Integer>();
		HashMap<HashSet<String>, Integer> pipair_pair = new HashMap<HashSet<String>, Integer>();
		
		/* construct pipair_single and pipair_pair from pipair */
		for(Map.Entry<HashSet<String>, HashSet<String>> entry : pipair.entrySet()){
			HashSet<String> key = entry.getKey();
			HashSet<String> value = entry.getValue();
			int key_count = key.size();
			int value_count = value.size();
			if(key_count == 1){
				pipair_single.put(key, value_count);
			} else if(key_count == 2){
				pipair_pair.put(key, value_count);
			} else{
				System.err.println("Error");  ////// throw Exception
			}
		}
		
		/*  key_pair<HashSet<String>> -> value_pair<Integer>,
		 *  key_single<String> -> value_single<Integer>  
		 * 
		 *  key_pair<HashSet<String>> -> pair<HashSet<String>>, 
		 *  key_single<String> -> single<HashSet<String>> */
		for(Map.Entry<HashSet<String>, Integer> entry_single : pipair_single.entrySet()){
			int value_single = entry_single.getValue();
			HashSet<String> key_s = entry_single.getKey();
			String key_single = key_s.toString();
			key_single = key_single.replaceAll("\\[", "");
			key_single = key_single.replaceAll("\\]", "");
			
			if(value_single >= support){
				for(Map.Entry<HashSet<String>, Integer> entry_pair : pipair_pair.entrySet()){
					int value_pair = entry_pair.getValue();
					HashSet<String> key_pair = entry_pair.getKey();
					float confi = (float)value_pair / value_single;
					
					if(value_pair >= support && key_pair.contains(key_single) && confi >=confidence && confi <= 1){		
						HashSet<String> single = null;
						HashSet<String> pair = null;
						single = pipair.get(key_s);  // not key_single, it is a String not HashSet
						pair = pipair.get(key_pair);
						for(String s:single){
							if(!pair.contains(s)){
								/* printing bug */
								ArrayList<String> bug_pair_sort = new ArrayList<String>();
								for(String str:key_pair){
									bug_pair_sort.add(str);
								}
								Collections.sort(bug_pair_sort);
						
								String bug_pair = bug_pair_sort.toString();
								bug_pair = bug_pair.replaceAll("\\[", "(");
								bug_pair = bug_pair.replaceAll("\\]", ")");
								
								NumberFormat fmt = NumberFormat.getPercentInstance();
								fmt.setMinimumFractionDigits(2);
								fmt.setMaximumFractionDigits(2);
								
								System.out.println("bug: " + key_single + " in "+ s 
									+ ", pair: " + bug_pair
									+ ", support: " + value_pair
									+ ", confidence: " + fmt.format(confi)
									);
							}	
						}
					}
				}
			}
	
		} // end of for loop 
	} // end of function findBug
	
} // end of class PiPair
