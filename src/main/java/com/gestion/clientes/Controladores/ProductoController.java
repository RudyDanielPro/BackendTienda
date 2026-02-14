package com.gestion.clientes.Controladores;

import com.gestion.clientes.Entidades.Producto;
import com.gestion.clientes.Entidades.ProductoFoto;
import com.gestion.clientes.Repositorios.ProductoRepository;
import com.gestion.clientes.Servicios.CloudinaryService; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") 
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    // ============================================
    // 1. OBTENER TODOS LOS PRODUCTOS
    // ============================================
    @GetMapping("/productos")
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // ============================================
    // 2. CREAR PRODUCTO
    // ============================================
    @PostMapping("/admin/productos")
    public ResponseEntity<?> crearProducto(@RequestBody Map<String, Object> payload) {
        try {
            Producto producto = new Producto();
            producto.setNombre((String) payload.get("nombre"));
            producto.setPrecio(new BigDecimal(payload.get("precio").toString()));
            producto.setColor((String) payload.get("color"));
            producto.setCategoria((String) payload.get("categoria"));

            if (payload.get("tallas") != null) {
                List<Map<String, Object>> tallasReq = (List<Map<String, Object>>) payload.get("tallas");
                for (Map<String, Object> t : tallasReq) {
                    String tallaNom = (String) t.get("talla");
                    Integer stock = Integer.parseInt(t.get("stock").toString());
                    producto.addTalla(tallaNom, stock);
                }
            }

            Producto nuevo = productoRepository.save(producto);
            return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", "Error al crear: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================
    // 3. ACTUALIZAR PRODUCTO
    // ============================================
    @PutMapping("/admin/productos/{id}")
    public ResponseEntity<?> actualizarProducto(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return productoRepository.findById(id).map(p -> {
            p.setNombre((String) payload.get("nombre"));
            p.setPrecio(new BigDecimal(payload.get("precio").toString()));
            p.setColor((String) payload.get("color"));
            p.setCategoria((String) payload.get("categoria"));
            
            p.getTallas().clear();
            if (payload.get("tallas") != null) {
                List<Map<String, Object>> tallasReq = (List<Map<String, Object>>) payload.get("tallas");
                for (Map<String, Object> t : tallasReq) {
                    p.addTalla((String) t.get("talla"), Integer.parseInt(t.get("stock").toString()));
                }
            }
            
            productoRepository.save(p);
            return ResponseEntity.ok(p);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ============================================
    // 4. ELIMINAR PRODUCTO
    // ============================================
    @DeleteMapping("/admin/productos/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        return productoRepository.findById(id).map(p -> {
            productoRepository.delete(p);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ============================================
    // 5. SUBIR FOTO A CLOUDINARY
    // ============================================
    @PostMapping("/admin/productos/{id}/upload")
    public ResponseEntity<?> subirFoto(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {

        try {
            // 1. Verificar si el producto existe
            Optional<Producto> productoOpt = productoRepository.findById(id);
            if (productoOpt.isEmpty()) {
                return new ResponseEntity<>(Map.of("error", "Producto no encontrado"), HttpStatus.NOT_FOUND);
            }

            if (archivo.isEmpty()) {
                return new ResponseEntity<>(Map.of("error", "El archivo está vacío"), HttpStatus.BAD_REQUEST);
            }

            // 2. Subir a Cloudinary
            Map<?, ?> result = cloudinaryService.upload(archivo);
            
            // 3. Obtener la URL segura (HTTPS)
            String urlImagen = (String) result.get("secure_url");

            // 4. Guardar o actualizar la foto en el producto
            Producto producto = productoOpt.get();
            ProductoFoto foto = producto.getFoto();
            
            if (foto == null) {
                foto = new ProductoFoto();
            }
            
            foto.setRuta(urlImagen); // Guardamos la URL completa
            foto.setNombreArchivo(archivo.getOriginalFilename());
            
            producto.setFoto(foto);
            productoRepository.save(producto);

            // 5. Respuesta
            return new ResponseEntity<>(Map.of(
                "mensaje", "Foto subida con éxito",
                "url", urlImagen,
                "foto", foto
            ), HttpStatus.CREATED);

        } catch (IOException e) {
            return new ResponseEntity<>(Map.of("error", "Error de E/S: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", "Error general: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}