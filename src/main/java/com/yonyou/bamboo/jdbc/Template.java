package com.yonyou.bamboo.jdbc;

import java.beans.PropertyDescriptor;
import java.util.List;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.yonyou.bamboo.interceptor.SigninInterceptor;

public class Template extends JdbcTemplate {

    private static Logger log = Logger.getLogger(SigninInterceptor.class);
    public static final String CLASS = "class";
    public static final String SPACE = " ";
    public static final String LEFT_BRACKET = "(";
    public static final String RIGHT_BRACKET = ")";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String INSERT = "insert";
    public static final String VALUES = "values";
    public static final String UNDERLINE = "_";
    public static final String SELECT = "select";
    public static final String ASTERISK = "*";
    public static final String FROM = "from";
    public static final String WHERE = "where";
    public static final String IDENTICAL = "1=1";
    public static final String EQUAL = "=";
    public static final String AND = "and";

    private NamedParameterJdbcTemplate namedJdbcTemplate;

    public Template() {
        super();
    }

    public Template(DataSource dataSource) {
        super(dataSource);
        initNamedJdbcTemplate();
    }

    public Template(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
        initNamedJdbcTemplate();
    }

    public void initNamedJdbcTemplate() {
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(this);
    }

    public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder) throws DataAccessException {
        return namedJdbcTemplate.update(sql, paramSource, generatedKeyHolder);
    }

    public <T> T queryForObject(T t, Class<T> type) {
        try {
            return namedJdbcTemplate.queryForObject(queryJoin(t), new BeanPropertySqlParameterSource(t), new BeanPropertyRowMapper<T>(type));
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
        } catch (IncorrectResultSizeDataAccessException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public <T> List<T> query(T t, Class<T> type) {
        return namedJdbcTemplate.query(queryJoin(t), new BeanPropertySqlParameterSource(t), new BeanPropertyRowMapper<T>(type));
    }

    private <T> String queryJoin(T t) {
        StringBuffer sql = new StringBuffer();
        sql.append(SELECT).append(SPACE).append(ASTERISK).append(SPACE);
        sql.append(FROM).append(SPACE).append(t.getClass().getSimpleName().toLowerCase()).append(SPACE);
        sql.append(WHERE).append(SPACE).append(IDENTICAL).append(SPACE);
        PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(t.getClass());
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(t);
        for (int i = 0; i < pds.length; i++) {
            PropertyDescriptor pd = pds[i];
            String name = pd.getName();
            if (!CLASS.equals(name) && beanWrapper.isReadableProperty(name) && beanWrapper.getPropertyValue(name) != null) {
                sql.append(AND).append(SPACE).append(underscoreName(name)).append(SPACE);
                sql.append(EQUAL).append(SPACE).append(COLON).append(name).append(SPACE);
            }
        }
        return sql.toString();
    }

    public <T> int insert(T t) {
        StringBuffer top = new StringBuffer();
        StringBuffer bot = new StringBuffer();
        top.append(INSERT).append(SPACE).append(t.getClass().getSimpleName().toLowerCase()).append(SPACE).append(LEFT_BRACKET);
        bot.append(VALUES).append(LEFT_BRACKET);
        PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(t.getClass());
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(t);
        for (int i = 0; i < pds.length; i++) {
            PropertyDescriptor pd = pds[i];
            String name = pd.getName();
            if (!CLASS.equals(name) && beanWrapper.isReadableProperty(name) && beanWrapper.getPropertyValue(name) != null) {
                top.append(underscoreName(name));
                bot.append(COLON).append(name);
                if (i < pds.length - 1) {
                    bot.append(COMMA).append(SPACE);
                    top.append(COMMA).append(SPACE);
                }
            }
        }
        top.append(RIGHT_BRACKET);
        bot.append(RIGHT_BRACKET);
        String sql = top.append(bot).toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        this.update(sql, new BeanPropertySqlParameterSource(t), keyHolder);
        return keyHolder.getKey().intValue();
    }

    /**
     * Convert a name in camelCase to an underscored name in lower case. Any upper case letters are converted to lower case with a preceding underscore.
     * 
     * @param name
     *            the string containing original name
     * @return the converted name
     */
    private String underscoreName(String name) {
        StringBuilder result = new StringBuilder();
        if (name != null && name.length() > 0) {
            result.append(name.substring(0, 1).toLowerCase());
            for (int i = 1; i < name.length(); i++) {
                String s = name.substring(i, i + 1);
                if (s.equals(s.toUpperCase())) {
                    result.append(UNDERLINE);
                    result.append(s.toLowerCase());
                } else {
                    result.append(s);
                }
            }
        }
        return result.toString();
    }

}