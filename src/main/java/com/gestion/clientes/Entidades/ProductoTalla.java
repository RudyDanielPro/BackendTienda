package com.gestion.clientes.Entidades;

import com.fasterxml.jackson.annotation.JsonBackReference; // Importante
import jakarta.persistence.*;

@Entity
@Table(name = "Producto_tallas")
public class ProductoTalla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonBackReference // <-- CORRECCIÓN: Evita que la talla intente serializar al producto de vuelta
    private Producto producto;

    @Column(nullable = false, length = 20)
    private String talla;

    @Column(nullable = false)
    private Integer stock;

    // Constructores
    public ProductoTalla() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}