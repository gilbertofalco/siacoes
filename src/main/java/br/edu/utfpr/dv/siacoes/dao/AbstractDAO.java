package br.edu.utfpr.dv.siacoes.dao;

import java.sql.SQLException;
import java.util.List;

public abstract class AbstractDAO<T> {

        public abstract T loadObject(ResultSet rs)  throws SQLException;
	public abstract int save(int id, T t) throws SQLException;
	public abstract T findById(int id) throws SQLException;
	public abstract List<T> listAll(boolean b) throws SQLException;
        public abstract List<T> listAll() throws SQLException;
}