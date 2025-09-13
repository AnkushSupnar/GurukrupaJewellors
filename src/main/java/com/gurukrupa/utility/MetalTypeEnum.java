package com.gurukrupa.utility;

public enum MetalTypeEnum {
    GOLD("Gold"),
    SILVER("Silver"),
    PLATINUM("Platinum"),
    DIAMOND("Diamond"),
    OTHER("Other");
    
    private final String displayName;
    
    MetalTypeEnum(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
    
    public static MetalTypeEnum fromString(String text) {
        if (text != null) {
            for (MetalTypeEnum metal : MetalTypeEnum.values()) {
                if (text.equalsIgnoreCase(metal.name()) || text.equalsIgnoreCase(metal.displayName)) {
                    return metal;
                }
            }
        }
        return OTHER;
    }
}