package it.polito.tdp.extflightdelays.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {
	
	//ci serve un grafo semplice, pesato, non orientato
	//in cui i vertici sono gli aereoporti
	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	
	//identity map che serve per usare direttamente gli oggetti nella
	//costruzione delle rotte
	private Map<Integer, Airport> idMap;
	
	//dao per la connessione con il database
	private ExtFlightDelaysDAO dao;
	
	//variabile che rigaurda l'albero di visita e che si salva (non in ordine perche' e' un hash e quindi
	//non mantiene l'ordine di inserimento) le singole relazioni padre e figlio, cioe' chi viene prima e 
	//chi viene dopo nella visita
	private Map<Airport,Airport> visita = new HashMap<>();
		
	
	public Model() {
		//potremmo anche non farlo qui nel costruttore, ma creare un metodo apposta
		//nel caso i dati cambiassero, ma non e' cosi' fondamentale
		idMap = new HashMap<Integer,Airport>();
		dao = new ExtFlightDelaysDAO();
		
		//invece di farci ritornare la lista dal metodo, gli facciamo direttamente aggiungere
		//quello che trova nella nostra mappa di aereoporti che ci creiamo
		this.dao.loadAllAirports(idMap);
	}
	
	public void creaGrafo(int x) {
		
		//ogni volta che l'utente preme il pulsante il grafo viene creato da zero secondo
		//le specifiche del problema
		this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		//aggiungiamo i vertici
		for(Airport a : idMap.values()) {
			
			//numero di compagnie su cui opera l'aereoporto maggiore della richiesta dell'utente
			if(dao.getAirlinesNumber(a) > x) {
				//inserisco aeroporto come vertice
				this.grafo.addVertex(a);
			}
			
			/*
			 	Per inserire un arco con il peso fa una cosa diversa da come l'ho fatta io nel laboratorio 8
			 	anche perche' il peso qui non e' piu' la distanza media, ma e' il numero di voli che connettono
			 	due aereoporti.
			 	
			 	Lui si fa dare tutte le rotte che ci sono e poi le prende tutte una alla volta (e ricordiamo
			 	che come anche io le avevo importate per ora sono unidirezionali ancora). Controlla se tra
			 	quei due aereoporti c'era gia' un arco e se si significa che avevamo gia' considerato tutte le 
			 	tratte nella direzione opposta e quello che vado a fare e' semplicemente quello di andarmi a 
			 	modificare il peso aggiornandolo e creando un nuovo arco che sostituisce quello precedente.
			 	Se per caso la rotta che sto considerando non ha ancora nessun arco associato, allora vado
			 	a crearmi l'arco con per ora il peso parziale che ho in una singola direzione.
			 	
			 	Qui si puo' fare cosi' perche' c'e' una differenza con quello che c'era in laboratorio in quanto
			 	in laboratorio il peso era la distanza media, mentre qui e' solo il numero di connessioni tra 
			 	i due e dunque si puo' fare questo tipo di approccio mentre la distanza media non si poteva
			 	calcolare parzialmente.
			 */
			for(Rotta r : dao.getRotte(idMap)) {
				
				//necessario avere questo controllo perche' se nella rotta che stiamo analizzando abbiamo
				//degli aereoporti che avevamo escluso, putroppo con l'istruzione 
				//Graphs.addEdgeWithVertices
				//il grafo ce li aggiungerebbe per noi e per evitare questo eseguiamo quell'istruzione di
				//creazione dell'arco solo se e' effettivamente tra due vertici che abbiamo inserito.
				//Nella creazione delle rotte noi avevamo creato le rotte tra tutti gli aereoporti e non
				//avevamo considerato quelli effettivamente creati ed inserito nel grafo secondo il vincolo
				//che l'utente inserisce.
				if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
					
					DefaultWeightedEdge e = this.grafo.getEdge(r.a1, r.a2);
					if(e == null) {
						Graphs.addEdgeWithVertices(this.grafo, r.getA1(), r.getA2(), r.getPeso());
					} else {
						double pesoVecchio = this.grafo.getEdgeWeight(e);
						double pesoNuovo = pesoVecchio + r.getPeso();
						//essendo il grafo non orientato, il nuovo arco sostituisce quello vecchio
						this.grafo.setEdgeWeight(e, pesoNuovo);
						
					}
				}
				
			}
		}
		
	}
	
	public int vertexNumber() {
		return this.grafo.vertexSet().size();
	}
	
	public int edgeNumber() {
		return this.grafo.edgeSet().size();
	}
	
	public Collection<Airport> getAeroporti(){
		return this.grafo.vertexSet();
	}
	
	/*
	 	Per trovare il percorso devo visitare il grafo e posso farla in ampiezza oppure in profondita'.
	 	Ci serve l'iteratore che scorre (in questo caso in ampiezza), ma dobbiamo anche implementare 
	 	l'interfaccia che ci permette di aggiungere alla mappa dell'albero, il lato che man mano l'iteratore
	 	sta scorrendo.
	 */
	public List<Airport> trovaPercorso(Airport a1, Airport a2){
		//dall'albero di visita che parte da a1 andremo ad estrapolarci se c'e' solamente il percorso
		//che termina in a2
		List<Airport> percorso = new ArrayList<Airport>();
		
		//specifichiamo il grafo e la partenza
		BreadthFirstIterator<Airport, DefaultWeightedEdge> it = new BreadthFirstIterator<>(this.grafo,a1);
		
		
		//aggiungo la "radice" del mio albero di visita (variabile creata globale nel model)
		visita.put(a1, null);
		
		//agganciamo il TraversalListener con una classe anonima per tenere traccia e costuirci
		//l'albero di visita
		it.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
				// TODO Auto-generated method stub
				
			}

	
			
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				//quando attraversiamo un arco, ci salviamo la relazione padre figlio
				//in cui il figlio e' la chiave e il padre e' il valore
				Airport sorgente = grafo.getEdgeSource(e.getEdge());
				Airport destinazione = grafo.getEdgeTarget(e.getEdge());
				
				//se la visita non contiene la destinazione, ma contiene il nodo sorgente
				//significa che correttamente dobbiamo creare la visita come anche la visita la
				//dobbiamo creare al contrario se c'e' destinazione, ma non c'e' sorgente in quanto
				//noi abbiamo importato destinazione e sorgente dall'arco preso dal grafo, ma essendo
				//che stiamo lavorando con un grafo non orientato non sappiamo bene l'ordine con
				//cui stiamo percorrendo l'arco.
				//Il concetto e' che non bisogna salvarsi l'arco se l'arco attuale e' un arco inutile
				//ai fini del percorso, e cioe' quando entrambi i nodi analizzati erano gia' stati 
				//percorsi nella visita, perche' in quel caso stiamo creando un ciclo se aggiungiamo un
				//arco di questo tipo.
				if(!visita.containsKey(destinazione) && visita.containsKey(sorgente))
					visita.put(destinazione,sorgente);
				else if (!visita.containsKey(sorgente) && visita.containsKey(destinazione)){
					visita.put(sorgente, destinazione);
				}

			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		//faccio andare l'iteratore e creandomi l'albero di visita globale
		//in quanto ogni volta che l'iteratore si muove, viene richiamata la classe
		//anonima che abbiamo implementato.
		while(it.hasNext()) {
			//finche' c'e' un prossimo nella lista vado avanti
			it.next();
		}
		
		//secondo me questo controllo andava solamente fatto su a2 in quanto a1 lo
		//aggiungiamo all'inizio come prima cosa in visita e quindi lui c'e' per
		//forza, mentre a2 non e' detto che ci sia e che quindi esista un percorso 
		//tra i due aereoporti
		if(!visita.containsKey(a1) || !visita.containsKey(a2)) {
			//i due aeroporti non sono collegati
			return null;
		}
		
		//in visita ci sono tutti i percorsi di visita verso tutti i nodi mentre noi vogliamo 
		//avere solo quello verso a2 logicamente
		Airport step = a2;
		
		//facciamo la creazione del percorso partendo dalla fine e tornando indietro nella
		//mappa dell'albero di visita man mano e costruendo tutti gli archi.
		while(!step.equals(a1)) {
			percorso.add(step);
			//estraggo il padre dell'ultimo che ho aggiunto nel percorso
			step = visita.get(step);
		}
		
		//aggiungo la partenza altrimenti non veniva aggiunta
		percorso.add(a1);
		
		return percorso;
		
	}
	
   
	
	
	
}
