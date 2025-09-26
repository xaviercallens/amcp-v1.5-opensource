package io.amcp.mobility;

import java.io.Serializable;

/**
 * Options for agent migration operations.
 * 
 * @author AMCP Development Team  
 * @version 1.4.0
 * @since 1.4.0
 */
public final class MigrationOptions implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static MigrationOptions defaultOptions() {
        return new MigrationOptions();
    }
    
}