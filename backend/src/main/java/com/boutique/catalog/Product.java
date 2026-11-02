package com.boutique.catalog;

import java.math.BigDecimal;

record Product(Long id, String name, String description, BigDecimal price, int stock) {

}
