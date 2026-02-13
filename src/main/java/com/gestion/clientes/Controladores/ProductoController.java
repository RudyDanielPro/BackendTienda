package com.gestion.clientes.Controladores;

import com.gestion.clientes.Entidades.Producto;
import com.gestion.clientes.Entidades.ProductoFoto;
import com.gestion.clientes.Repositorios.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Value("${uploads.directory}")
    private String uploadsDirectory;

    // ============================================
    // 1. OBTENER TODOS LOS PRODUCTOS (Público)
    // ============================================
    @GetMapping("/productos")
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // ============================================
    // 2. CREAR PRODUCTO (Arregla el Error 404)
    // ============================================
    @PostMapping("/admin/productos")
    public ResponseEntity<?> crearProducto(@RequestBody Map<String, Object> payload) {
        try {
            Producto producto = new Producto();
            producto.setNombre((String) payload.get("nombre"));
            producto.setPrecio(new BigDecimal(payload.get("precio").toString()));
            producto.setColor((String) payload.get("color"));
            producto.setCategoria((String) payload.get("categoria"));

            // Procesar tallas enviadas desde React
            if (payload.get("tallas") != null) {
                List<Map<String, Object>> tallasReq = (List<Map<String, Object>>) payload.get("tallas");
                for (Map<String, Object> t : tallasReq) {
                    String tallaNom = (String) t.get("talla");
                    Integer stock = (Integer) t.get("stock");
                    producto.addTalla(tallaNom, stock);
                }
            }

            Producto nuevo = productoRepository.save(producto);
            return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al crear: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
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
            
            // Nota: La actualización de tallas suele requerir limpiar la lista previa
            p.getTallas().clear();
            if (payload.get("tallas") != null) {
                List<Map<String, Object>> tallasReq = (List<Map<String, Object>>) payload.get("tallas");
                for (Map<String, Object> t : tallasReq) {
                    p.addTalla((String) t.get("talla"), (Integer) t.get("stock"));
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
    // 5. SUBIR FOTO A UN PRODUCTO
    // ============================================
    @PostMapping("/admin/productos/{id}/upload")
    public ResponseEntity<?> subirFoto(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {

        try {
            Optional<Producto> productoOpt = productoRepository.findById(id);
            if (!productoOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("mensaje", "Producto no encontrado");
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }

            if (archivo.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "El archivo está vacío");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            String contentType = archivo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Solo se permiten imágenes");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            String nombreOriginal = archivo.getOriginalFilename();
            String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            String nombreUnico = UUID.randomUUID().toString() + extension;

            File directorio = new File(uploadsDirectory);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            String rutaCompleta = uploadsDirectory + File.separator + nombreUnico;
            Path ruta = Paths.get(rutaCompleta);
            Files.write(ruta, archivo.getBytes());

            ProductoFoto foto = new ProductoFoto();
            foto.setRuta("/uploads/" + nombreUnico); // Ajustado para que el GET lo encuentre
            foto.setNombreArchivo(nombreOriginal);

            Producto producto = productoOpt.get();
            producto.setFoto(foto);
            productoRepository.save(producto);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Foto subida exitosamente");
            respuesta.put("foto", foto);

            return new ResponseEntity<>(respuesta, HttpStatus.CREATED);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error de E/S: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================
    // 6. SERVIDOR DE ARCHIVOS - VER FOTO
    // ============================================
    @GetMapping("/uploads/{nombreArchivo}")
    public ResponseEntity<byte[]> verFoto(@PathVariable String nombreArchivo) {
        try {
            Path ruta = Paths.get(uploadsDirectory + File.separator + nombreArchivo);
            byte[] imagen = Files.readAllBytes(ruta);
            return ResponseEntity.ok().body(imagen);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}