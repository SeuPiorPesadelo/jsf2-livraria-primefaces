package br.com.caelum.livraria.bean;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.primefaces.component.datatable.DataTable;

import br.com.caelum.livraria.dao.DAO;
import br.com.caelum.livraria.modelo.Autor;
import br.com.caelum.livraria.modelo.Livro;
import br.com.caelum.livraria.modelo.LivroDataModel;
import br.com.caelum.livraria.util.RedirectView;

@ManagedBean
// @RequestScoped //� o padr�o
@ViewScoped
public class LivroBean {

	private Integer livroId;
	private Livro livro = new Livro();
	private Integer autorId;
	private List<Livro> livros = new DAO<Livro>(Livro.class).listaTodos();
	private EntityManager em;
	private Livro livroSelecionado;
	private LivroDataModel livroDataModel = new LivroDataModel();
	private List<String> generos = Arrays.asList("Romance", "Drama", "A��o");

	public List<String> getGeneros() {
	    return generos;
	}
	
	public Integer getLivroId() {
		return livroId;
	}

	public void setLivroId(Integer livroId) {
		this.livroId = livroId;
	}

	public Livro getLivro() {
		return livro;
	}

	public List<Autor> getAutores() {
		return new DAO<Autor>(Autor.class).listaTodos();
	}

	public Livro getLivroSelecionado() {
		return livroSelecionado;
	}

	public void setLivroSelecionado(Livro livroSelecionado) {
		this.livroSelecionado = livroSelecionado;
	}

	public LivroDataModel getLivroDataModel() {
		return livroDataModel;
	}

	public void setLivroDataModel(LivroDataModel livroDataModel) {
		this.livroDataModel = livroDataModel;
	}

	public void gravar() {
		System.out.println("Gravando livro " + this.livro.getTitulo());
		if (livro.getAutores().isEmpty()) {
			// p/ personalizacao de mensagem � necess�rio no 1� param o client
			// Id, e no 2� a mensagem
			FacesContext.getCurrentInstance().addMessage("autor",
					new FacesMessage("Livro deve ter pelo menos um autor"));
		}
		if (this.livro.getId() == null) {
			new DAO<Livro>(Livro.class).adiciona(this.livro);
		} else {
			new DAO<Livro>(Livro.class).atualiza(this.livro);
		}
		livros = null;
		livro = new Livro();
		
//		faz resetar os valores do filtro da Criteria da dataTable de livros
		DataTable dataTable = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent("formTabelaLivros:tabelaLivros");
		dataTable.reset();
	}

	public void gravarAutor() {
		Autor buscaPorId = new DAO<Autor>(Autor.class).buscaPorId(autorId);
		livro.adicionaAutor(buscaPorId);
	}

	public List<Autor> getAutoresDoLivro() {
		return this.livro.getAutores();
	}

	public Integer getAutorId() {
		return autorId;
	}

	public void setAutorId(Integer autorId) {
		this.autorId = autorId;
	}

	// FacesContext obtem info. da view processada no momento
	// UIComponent pega o componente q est� sendo validado
	// Object pega o valor q o usuario digitou
	public void comecaComDigitoUm(FacesContext ctx, UIComponent comp, Object value) throws ValidatorException {
		String v = value.toString();
		if (!v.startsWith("1")) {
			// ValidatorException diz ao JSF q algo deu errado
			throw new ValidatorException(new FacesMessage("Deve come�ar com 1"));
		}
	}

	public List<Livro> getLivros() {
		System.out.println("chamou getLivros()");
		if (livros == null) {
			livros = new DAO<Livro>(Livro.class).listaTodos();
		}
		return livros;
	}

	public void setLivros(List<Livro> livros) {
		this.livros = livros;
	}

	public RedirectView formAutor() {
		System.out.println("Executou o formAutor");
		// ?faces-redirect=true faz a URI atualizar
		// pq for�a um segundo request p/ o xhtml correspondente
		// return "autor?faces-redirect=true";
		return new RedirectView("autor");
	}

	public void carregar(Livro l) {
		System.out.println("carregou");
		em = new DAO<Livro>(Livro.class).getEntityManager();
		// busca com join fetch, pq o relacionamento � lazy
		TypedQuery<Livro> query = em.createQuery("SELECT l FROM Livro l JOIN FETCH l.autores a WHERE l.id = :id",
				Livro.class);
		query.setParameter("id", l.getId());
		this.livro = query.getResultList().get(0);
		em.close();
	}

	public void carregaPelaId() {
		em = new DAO<Livro>(Livro.class).getEntityManager();
		// busca com join fetch, pq o relacionamento � lazy
		TypedQuery<Livro> query = em.createQuery("SELECT l FROM Livro l JOIN FETCH l.autores a WHERE l.id = :id",
				Livro.class);
		query.setParameter("id", livroId);
		try {
			this.livro = query.getResultList().get(0);
		} catch (Exception e) {
			throw new ValidatorException(new FacesMessage("Passe um id v�lido"));
		}
		em.close();
	}

	public void remover(Livro l) {
		System.out.println("deletou");
		new DAO<Livro>(Livro.class).remove(l);
		livros.remove(l);
	}

	public void removerAutorDoLivro(Autor a) {
		livro.removeAutor(a);
	}

	// primeiro par�metro � o valor da coluna, o segundo � o filtro, o terceiro
	// define a locale
	public boolean precoEhMenor(Object valorColuna, Object filtroDigitado, Locale locale) { // java.util.Locale
		// tirando espa�os do filtro
		String textoDigitado = (filtroDigitado == null) ? null : filtroDigitado.toString().trim();

		System.out.println("Filtrando pelo " + textoDigitado + ", Valor do elemento: " + valorColuna);

		// o filtro � nulo ou vazio?
		if (textoDigitado == null || textoDigitado.equals("")) {
			return true;
		}

		// elemento da tabela � nulo?
		if (valorColuna == null) {
			return false;
		}

		try {
			// fazendo o parsing do filtro para converter para Double
			Double precoDigitado = Double.valueOf(textoDigitado);
			Double precoColuna = (Double) valorColuna;

			// comparando os valores, compareTo devolve um valor negativo se o
			// value � menor do que o filtro
			return precoColuna.compareTo(precoDigitado) < 0;

		} catch (NumberFormatException e) {
			// usuario nao digitou um numero
			return false;
		}
	}
}
