package Main;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

public class CustomPrettyPrinter extends DefaultPrettyPrinter {
    private String objectFieldValueSeparator = " : ";
    public CustomPrettyPrinter withObjectFieldValueSeparator(String separator){
        objectFieldValueSeparator = separator;
        return this;
    }

    public CustomPrettyPrinter(){}
    private CustomPrettyPrinter(String objectFieldValueSeparator){
        this.objectFieldValueSeparator = objectFieldValueSeparator;
    }

    @Override
    public DefaultPrettyPrinter createInstance(){
        return new CustomPrettyPrinter(objectFieldValueSeparator);
    }
    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException{
        jg.writeRaw(objectFieldValueSeparator);
    }
}
