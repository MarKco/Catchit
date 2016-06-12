package pl.surecase.eu;

import java.io.File;
import java.util.HashMap;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

/**
 * Created by marco on 26/06/15.
 */
public abstract class AbstractGeneratorHelper {

    //  The version of the schema that will be created
    protected static int VERSION = 1;
    //  The package of the classes created
    protected static String BASE_PACKAGE_NAME = "";
    //  The sub-package where will be saved the entities created
    protected String DAO_ENTITIES_PACKAGE_NAME = "database";
    //  The package where are super class of entities (these classes can change the way you like!)
    protected String SUPER_CLASSES_PACKAGE_NAME = "classes";

    protected Schema schema = null;
    protected HashMap<String,Entity> ENTITIES = new HashMap<>();

    public AbstractGeneratorHelper(){
        this( VERSION, BASE_PACKAGE_NAME);
    }

    public AbstractGeneratorHelper( int version, String base_package_name ){

        try {
            if( base_package_name == null || base_package_name.equals("") ){
                base_package_name = getClass().getPackage().getName();
            }

            setVersion(version);
            setPackageName(base_package_name);

            createSchema();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createSchema(){
        schema = new Schema( getVersion(), getPackageName() +"."+ DAO_ENTITIES_PACKAGE_NAME);
    }

    protected void addEntity( String entity_name, Entity entity ){
        ENTITIES.put(entity_name, entity);
    }

    protected Entity getEntity( String entity_name ){
        return ENTITIES.get(entity_name);
    }

    public void createClasses(){
        createClasses("classesGenerated/"+ getPackageName() +"/" );
    }

    public void createClasses( String path_where_create_classes ){
        try{

            File path = new File(path_where_create_classes);
            String absolute_path = path.getAbsolutePath();

            if( !path.exists() && !(new File(path_where_create_classes)).mkdirs() ) {
                //  Directory creation failed
                throw new Exception("It was impossible to create the folder in which to save the generated entities from the library. Please create hand the path or assign the appropriate permissions.\n The path is: "+ absolute_path);
            }

            System.out.println("\n"+"The path where will create the entities class is: "+ absolute_path +"\n\n");

            new DaoGenerator().generateAll( getSchema(), path_where_create_classes );

//			Iterator it = ENTITIES.entrySet().iterator();
//		    while( it.hasNext() ){
//		        Map.Entry pairs = (Map.Entry) it.next();
//
//		        String entityName = (String) pairs.getKey();
//		        Entity value = (Entity) pairs.getValue();
//
//
//		        System.out.println( entityName + " = " + value.getTableName() );
//		    }

        }catch( Exception e ){
            e.printStackTrace();
        }
    }

    /**
     *  Create and return an Entity Object and setting to it the table name (same of object_name), the super class and
     *  set to it the serializable implementation.
     *
     *  @param  object_name     It will be the name of the entity class and the table in database
     *
     *  @see    #createEntity( String, String )
     *  @see    de.greenrobot.daogenerator.Entity
     */
    protected Entity createEntity( String object_name ){
        return createEntity( object_name, object_name );
    }

    /**
     *  Create and return an Entity Object and setting to it the table name, the super class and
     *  set to it the serializable implementation.
     *
     *  @param  object_name     The name of the entity class
     *  @param  table_name      The name of the table in database
     *
     *  @see    #createEntity( String, String )
     *  @see    de.greenrobot.daogenerator.Entity
     */
    protected Entity createEntity( String object_name, String table_name ){
        return createEntity( object_name, table_name, getPackageName() +"."+ SUPER_CLASSES_PACKAGE_NAME);
    }

    /**
     *  Create and return an Entity Object and setting to it the table name, the super class and
     *  set to it the serializable implementation.
     *
     *  @param  object_name         The name of the entity class
     *  @param  table_name          The name of the table in database
     *  @param  package_superclass  The package to assign to the entity class. This allow you to
     *                              modify the super class -senza preoccuparti-
     *
     *  @see    #createEntity( String, String )
     *  @see    de.greenrobot.daogenerator.Entity
     */
    protected Entity createEntity( String object_name, String table_name, String package_superclass ){

        Entity entity = getSchema().addEntity( object_name );
        entity.setTableName( table_name );
        entity.setSuperclass( package_superclass +".Super"+ object_name );
        entity.implementsSerializable();

        return entity;
    }



    /////////////////////////////////////
    //	Getters and Setters

    public int getVersion() {
        return VERSION;
    }
    public void setVersion( int version ){
        this.VERSION = version;
    }

    public String getPackageName() {
        return BASE_PACKAGE_NAME;
    }
    public void setPackageName(String package_name){
        this.BASE_PACKAGE_NAME = package_name;
    }

    public Schema getSchema() {
        return schema;
    }
    public void setSchema( Schema schema ){
        this.schema = schema;
    }

}
