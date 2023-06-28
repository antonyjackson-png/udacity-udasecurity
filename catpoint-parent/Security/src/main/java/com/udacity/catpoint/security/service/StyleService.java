package com.udacity.catpoint.security.service;

import java.awt.*;

/**
 * Simple "service" for providing style information.
 */
public class StyleService {

    // Changed HEADING_FONT to final because SpotBugs reported it as High Priority in the MALICIOUS_CODE category
    public static final Font HEADING_FONT = new Font("Sans Serif", Font.BOLD, 24);

}
