package com.gestion.clientes.Entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "producto_fotos")
public class ProductoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String ruta;

    @Column(length = 255)
    private String nombreArchivo;

    public ProductoFoto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
}