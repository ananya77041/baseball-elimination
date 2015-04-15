import java.util.HashMap;
import java.util.Iterator;

public class BaseballElimination {
	private int numTeams;
	private HashMap<String, Integer[]> teams;
	private HashMap<Integer, String> teamNumbers;
	private int[][] against;
	private Bag<String> subset;
	private int[] wins;
	private boolean checked;


	public BaseballElimination(String filename) {                    // create a baseball division from given filename in format specified below
		readDivision(filename);
	}

	private void readDivision(String filename) {
		teams = new HashMap<String, Integer[]>();	
		teamNumbers = new HashMap<Integer, String>();
		In in = new In(filename);
		numTeams = in.readInt();		
		against = new int[numTeams][numTeams];
		wins = new int[numTeams];

		int i = 0;
		while (!in.isEmpty()) {
			// read team name, number, wins, losses, and remaining to hashmap			
			String team = in.readString();
			Integer[] games = new Integer[4];
			games[0] = i;
			games[1] = in.readInt();
			wins[i] = games[1];
			games[2] = in.readInt();
			games[3] = in.readInt();	
			teams.put(team, games);
			teamNumbers.put(games[0], team);

			// add current team's future matches to matrix
			for (int j = 0; j < numTeams; j++) {
				against[i][j] = in.readInt();
			}			
			i++;
		}
	}

	public int numberOfTeams() {                       // number of teams
		return numTeams;	
	}

	public Iterable<String> teams() {                               // all teams
		return teams.keySet();
	}

	public int wins(String team) {                     // number of wins for given team
		checkTeam(team);
		return teams.get(team)[1];
	}

	public int losses(String team) {                   // number of losses for given team
		checkTeam(team);
		return teams.get(team)[2];
	}

	public int remaining(String team) {                // number of remaining games for given team
		checkTeam(team);
		return teams.get(team)[3];
	}

	public int against(String team1, String team2) {   // number of remaining games between team1 and team2
		checkTeam(team1); checkTeam(team2);
		return against[teams.get(team1)[0]][teams.get(team2)[0]];
	}

	public boolean isEliminated(String team) {             // is given team eliminated?
		checkTeam(team);

		boolean flag = false;
		subset = new Bag<String>();
		// trivial elimination
		for (Iterator<String> teams = teams().iterator(); teams.hasNext();) {
			String current = teams.next();
			if (wins(team) + remaining(team) < wins(current)) { 
				subset.add(current);
				checked = true;
				return true; 
			}			
		}
		flag = flowCheck(team);
		checked = true;
		return flag;
	}

	private boolean flowCheck(String team) {
		// initialize flow network with source [size-2] and sink [size-1]
		int gameCombos = numGameCombos(numTeams);
		int size = 2+numTeams+gameCombos-1;
		int teamNumber = teams.get(team)[0];
		FlowNetwork network = new FlowNetwork(size);

		// add edges for source to game combination vertices to teams
		int x = 0; int y = 0; int z = 0; int sum = 0; String[] networkTeams = new String[numTeams-1];
		for (int i = 0; i < numTeams; i++) {
			if (i != teamNumber) {				
				for (int j = 0; j < numTeams; j++) {
					if (j != teamNumber && j > i) {
						network.addEdge(new FlowEdge(size-2, x, against[i][j]));
						sum += against[i][j];
						network.addEdge(new FlowEdge(x, size-1-numTeams+y, Double.POSITIVE_INFINITY));
						network.addEdge(new FlowEdge(x, size-numTeams+z, Double.POSITIVE_INFINITY));				
						x++; z++;
					}					
				}
				
				int weight = wins(team) + remaining(team) - wins[i];
				network.addEdge(new FlowEdge(size-1-numTeams+y, size-1, weight));
				networkTeams[y] = teamNumbers.get(i);
				
				y++; z = y;				
			}
		}

		// calculate maxflow using FF
		FordFulkerson ff = new FordFulkerson(network, size-2, size-1);		
		if (sum == ff.value()) return false;
		else {
			for (int v = gameCombos; v < size-2; v++) {
				if (ff.inCut(v)) {					
					subset.add(networkTeams[v-gameCombos]);      	
				}
			}
			return true;
		}
	}

	private int numGameCombos(int x) {
		int n = x - 1;
		return n*(n-1)/2;
	}

	public Iterable<String> certificateOfElimination(String team) { // subset R of teams that eliminates given team; null if not eliminated
		checkTeam(team);
		
		boolean flag = isEliminated(team);
		if (flag) return subset;
		else return null;
	}

	private void checkTeam(String team) {
		if (!teams.containsKey(team)) throw new java.lang.IllegalArgumentException();
	}

	public static void main(String[] args) {
		BaseballElimination division = new BaseballElimination(args[0]);
		//		StdOut.println(division.numberOfTeams());
		//		StdOut.println(division.losses("Atlanta"));
		//		StdOut.println(division.wins("Montreal"));
		//		StdOut.println(division.remaining("New_York"));
		//		StdOut.println(division.against("Philadelphia", "Montreal"));
		//		StdOut.println(division.teams());		

		for (String team : division.teams()) {
			if (division.isEliminated(team)) {
				StdOut.print(team + " is eliminated by the subset R = { ");
				for (String t : division.certificateOfElimination(team)) {
					StdOut.print(t + " ");
				}
				StdOut.println("}");
			}
			else {
				StdOut.println(team + " is not eliminated");
			}
		}
	}

}
