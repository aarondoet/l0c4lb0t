package DataManager;

import lombok.Getter;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;

public class SafeResultSet {

    private List<Map<String, ResultSetEntry>> values = new ArrayList<>();
    @Getter private List<String> columnNames = new ArrayList<>();
    private int row = -1;
    public int getRowCount(){return values.size();}
    public int getColumnCount(){return columnNames.size();}
    public String getColumnName(int col){return columnNames.get(col-1);}

    public SafeResultSet(ResultSet rs){
        try{
            ResultSetMetaData md = rs.getMetaData();
            for(int i = 1; i <= md.getColumnCount(); i++) columnNames.add(md.getColumnName(i));
            while(rs.next()){
                Map<String, ResultSetEntry> vals = new HashMap<>();
                for(int i = 1; i <= md.getColumnCount(); i++)
                    vals.put(md.getColumnName(i), new ResultSetEntry(md.getColumnType(i), rs.getObject(i)));
                values.add(vals);
            }
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public boolean next(){
        return ++row < values.size();
    }

    public String getString(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        return entry.val.toString();
    }
    public String getString(int col){
        return getString(columnNames.get(col-1));
    }
    public int getInt(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return 0;
        if(entry.val instanceof Number) return ((Number)entry.val).intValue();
        try{
            return Integer.parseInt(entry.val.toString());
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public int getInt(int col){
        return getInt(columnNames.get(col-1));
    }
    public boolean getBoolean(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return false;
        if(entry.val instanceof Boolean) return (boolean)entry.val;
        if(entry.val instanceof Number) return !entry.val.equals(1);
        if(entry.val instanceof String) return ((String)entry.val).trim().toLowerCase().matches("^(true|1)$");
        throw new ClassCastException();
    }
    public boolean getBoolean(int col){
        return getBoolean(columnNames.get(col-1));
    }
    public long getLong(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return 0;
        if(entry.val instanceof Number) return ((Number)entry.val).longValue();
        try{
            return Long.parseLong(entry.val.toString());
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public long getLong(int col){
        return getLong(columnNames.get(col-1));
    }
    public Timestamp getTimestamp(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        if(entry.val instanceof Timestamp) return (Timestamp)entry.val;
        if(entry.val instanceof Instant) return Timestamp.from((Instant)entry.val);
        if(entry.val instanceof Long){
            try{
                return Timestamp.from(Instant.ofEpochMilli((long)entry.val));
            }catch(Exception ex){
                throw new ClassCastException();
            }
        }
        try{
            return Timestamp.from(Instant.parse(entry.val.toString()));
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public Timestamp getTimestamp(int col){
        return getTimestamp(columnNames.get(col-1));
    }
    public Object getObject(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        return entry.val;
    }
    public Object getObject(int col){
        return getObject(columnNames.get(col-1));
    }
    public Blob getBlob(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        if(entry.val instanceof Blob) return (Blob)entry.val;
        throw new ClassCastException();
    }
    public Blob getBlob(int col){
        return getBlob(columnNames.get(col-1));
    }
    public Clob getClob(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        if(entry.val instanceof Clob) return (Clob)entry.val;
        throw new ClassCastException();
    }
    public Clob getClob(int col){
        return getClob(columnNames.get(col-1));
    }
    public byte getByte(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return 0;
        if(entry.val instanceof Number) return ((Number)entry.val).byteValue();
        try{
            return Byte.parseByte(entry.val.toString());
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public byte getByte(int col){
        return getByte(columnNames.get(col-1));
    }
    public Array getArray(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        if(entry.val instanceof Array) return (Array)entry.val;
        throw new ClassCastException();
    }
    public Array getArray(int col){
        return getArray(columnNames.get(col-1));
    }
    public short getShort(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return 0;
        if(entry.val instanceof Short) return ((Number)entry.val).shortValue();
        try{
            return Short.parseShort(entry.val.toString());
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public short getShort(int col){
        return getShort(columnNames.get(col-1));
    }
    public double getDouble(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return 0;
        if(entry.val instanceof Short) return ((Number)entry.val).doubleValue();
        try{
            return Double.parseDouble(entry.val.toString());
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public double getDouble(int col){
        return getDouble(columnNames.get(col-1));
    }
    public float getFloat(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return 0;
        if(entry.val instanceof Short) return ((Number)entry.val).floatValue();
        try{
            return Float.parseFloat(entry.val.toString());
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public float getFloat(int col){
        return getFloat(columnNames.get(col-1));
    }
    public Date getDate(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        if(entry.val instanceof Date) return (Date)entry.val;
        if(entry.val instanceof Long) return new Date((long)entry.val);
        try{
            return Date.from(Instant.parse(entry.val.toString()));
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public Date getDate(int col){
        return getDate(columnNames.get(col-1));
    }
    public BigDecimal getBigDecimal(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        if(entry.val instanceof BigDecimal) return (BigDecimal)entry.val;
        if(entry.val instanceof Long) return BigDecimal.valueOf((long)entry.val);
        if(entry.val instanceof Double) return BigDecimal.valueOf((double)entry.val);
        try{
            return new BigDecimal(entry.val.toString());
        }catch(Exception ex){
            throw new ClassCastException();
        }
    }
    public BigDecimal getBigDecimal(int col){
        return getBigDecimal(columnNames.get(col-1));
    }
    public byte[] getBytes(String col){
        ResultSetEntry entry = values.get(row).get(col);
        if(entry == null) throw new NoSuchElementException();
        if(entry.val == null) return null;
        if(entry.val instanceof byte[]) return (byte[])entry.val;
        throw new ClassCastException();
    }
    public byte[] getBytes(int col){
        return getBytes(columnNames.get(col-1));
    }
    // time binarystream(inputstream) asciistream(inputstream) characterstream(reader) ncharacterstream(reader)
    // nclob nstring ref rowid sqlxml






    private static class ResultSetEntry {
        private int type;
        private Object val;
        ResultSetEntry(int type, Object val){
            this.type = type;
            this.val = val;
        }
    }

}
