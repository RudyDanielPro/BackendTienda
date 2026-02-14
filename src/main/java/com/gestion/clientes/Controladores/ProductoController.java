package com.gestion.clientes.Controladores;

import com.gestion.clientes.Entidades.Producto;
import com.gestion.clientes.Entidades.ProductoFoto;
import com.gestion.clientes.Entidades.ProductoTalla;
import com.gestion.clientes.Repositorios.ProductoRepository;
import com.gestion.clientes.Servicios.CloudinaryService;
import com.gestion.clientes.Servicios.EmailService;
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

    @Autowired
    private EmailService emailService;

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

    // ============================================
    // 6. NOTIFICAR COMPRA (ENVÍA EL CORREO AL ADMIN)
    // ============================================
    @PostMapping("/productos/notificar-compra")
    public ResponseEntity<?> notificarCompra(@RequestBody List<Map<String, Object>> carrito) {
        try {
            StringBuilder listaProd = new StringBuilder();
            StringBuilder links = new StringBuilder();
            
            // URL base de tu backend (Usando la URL de Render para que los links funcionen en móviles)
            String baseUrl = "https://backendtienda-yx56.onrender.com"; 

            for (Map<String, Object> item : carrito) {
                Map<String, Object> prod = (Map<String, Object>) item.get("product");
                
                Long idProd = Long.parseLong(prod.get("id").toString());
                String nombre = (String) prod.get("nombre");
                String talla = (String) item.get("selectedSize");

                listaProd.append("- ").append(nombre).append(" (Talla: ").append(talla).append(")\n");
                
                // Generamos el link que el admin clickeará para descontar 1 unidad
                String linkDeduccion = baseUrl + "/api/admin/productos/descontar-stock?productoId=" + idProd + "&talla=" + talla + "&cantidad=1";
                
                links.append("👉 Descontar 1 unid. de ").append(nombre).append(" (").append(talla).append("):\n")
                     .append(linkDeduccion).append("\n\n");
            }

            emailService.enviarNotificacionAdmin(listaProd.toString(), links.toString());
            
            return ResponseEntity.ok(Map.of("mensaje", "Notificación enviada al administrador"));
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", "Error al procesar la notificación: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================
    // 7. DESCONTAR STOCK (AL HACER CLIC EN EL CORREO)
    // ============================================
    @GetMapping("/admin/productos/descontar-stock")
    public ResponseEntity<String> descontarStock(
            @RequestParam Long productoId, 
            @RequestParam String talla, 
            @RequestParam Integer cantidad) {
        
        Optional<Producto> optProd = productoRepository.findById(productoId);
        
        if (optProd.isPresent()) {
            Producto producto = optProd.get();
            boolean modificado = false;
            
            for (ProductoTalla pt : producto.getTallas()) {
                if (pt.getTalla().equalsIgnoreCase(talla)) {
                    int nuevoStock = pt.getStock() - cantidad;
                    pt.setStock(Math.max(0, nuevoStock)); // Evita stock negativo
                    modificado = true;
                    break;
                }
            }
            
            if (modificado) {
                productoRepository.save(producto);
                return ResponseEntity.ok("✅ Stock descontado exitosamente. Producto: " + producto.getNombre() + " | Talla: " + talla);
            } else {
                return ResponseEntity.badRequest().body("❌ Talla no encontrada en el producto.");
            }
        }
        
        return ResponseEntity.notFound().build();
    }
}