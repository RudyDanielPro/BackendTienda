package com.gestion.clientes.Entidades;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(length = 100)
    private String color;

    @Column(length = 255)
    private String categoria;

    // --- Único cambio: campo descripción ---
    @Column(length = 1000)
    private String descripcion;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductoTalla> tallas = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "foto_id")
    @JsonManagedReference // 👈 Envía los datos de la foto al Frontend
    private ProductoFoto foto;

    public Producto() {}

    public void addTalla(String tallaNombre, Integer stock) {
        ProductoTalla pt = new ProductoTalla();
        pt.setTalla(tallaNombre);
        pt.setStock(stock);
        pt.setProducto(this);
        this.tallas.add(pt);
    }

    public void addTalla(ProductoTalla talla) {
        if (talla != null) {
            tallas.add(talla);
            talla.setProducto(this);
        }
    }

    public void removeTalla(ProductoTalla talla) {
        tallas.remove(talla);
        talla.setProducto(null);
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    // --- Único cambio: Getter y Setter de descripción ---
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<ProductoTalla> getTallas() { return tallas; }
    public void setTallas(List<ProductoTalla> tallas) { 
        this.tallas.clear();
        if (tallas != null) tallas.forEach(this::addTalla);
    }
    public ProductoFoto getFoto() { return foto; }
    public void setFoto(ProductoFoto foto) {
        if (foto != null) foto.setProducto(this);
        this.foto = foto;
    }
}