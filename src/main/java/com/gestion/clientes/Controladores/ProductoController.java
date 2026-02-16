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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
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
            
            if (payload.get("descripcion") != null) {
                producto.setDescripcion((String) payload.get("descripcion"));
            }

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
            
            if (payload.get("descripcion") != null) {
                p.setDescripcion((String) payload.get("descripcion"));
            }
            
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
            Optional<Producto> productoOpt = productoRepository.findById(id);
            if (productoOpt.isEmpty()) {
                return new ResponseEntity<>(Map.of("error", "Producto no encontrado"), HttpStatus.NOT_FOUND);
            }

            if (archivo.isEmpty()) {
                return new ResponseEntity<>(Map.of("error", "El archivo está vacío"), HttpStatus.BAD_REQUEST);
            }

            Map<?, ?> result = cloudinaryService.upload(archivo);
            String urlImagen = (String) result.get("secure_url");

            Producto producto = productoOpt.get();
            ProductoFoto foto = producto.getFoto();
            
            if (foto == null) {
                foto = new ProductoFoto();
            }
            
            foto.setRuta(urlImagen); 
            foto.setNombreArchivo(archivo.getOriginalFilename());
            
            producto.setFoto(foto);
            productoRepository.save(producto);

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
    // 6. NOTIFICAR COMPRA (CON ASUNTO ÚNICO)
    // ============================================
    @PostMapping("/productos/notificar-compra")
    public ResponseEntity<?> notificarCompra(@RequestBody List<Map<String, Object>> carrito) {
        try {
            StringBuilder listaProd = new StringBuilder();
            StringBuilder links = new StringBuilder();
            String baseUrl = "https://backendtienda-yx56.onrender.com"; 

            for (Map<String, Object> item : carrito) {
                // Validación para evitar NullPointerException si falta el ID
                if (item.get("id") == null) continue;

                Long idProd = Long.parseLong(item.get("id").toString());
                String nombre = (String) item.get("nombre");
                String talla = (String) item.get("talla"); 
                Integer cantidad = item.get("cantidad") != null ? Integer.parseInt(item.get("cantidad").toString()) : 1;

                String tallaEncoded = URLEncoder.encode(talla, StandardCharsets.UTF_8);

                listaProd.append("- ").append(nombre)
                         .append(" (Talla: ").append(talla)
                         .append(") x").append(cantidad).append("\n");
                
                String linkDeduccion = baseUrl + "/api/admin/productos/descontar-stock?productoId=" + idProd + "&talla=" + tallaEncoded + "&cantidad=" + cantidad;
                
                links.append("<div style='margin-bottom: 15px; border-bottom: 1px solid #eee; padding-bottom: 10px;'>")
                     .append("<p><strong>Producto:</strong> ").append(nombre)
                     .append(" | <strong>Talla:</strong> ").append(talla)
                     .append(" | <strong>Cantidad:</strong> ").append(cantidad).append("</p>")
                     .append("<a href='").append(linkDeduccion).append("' style='background-color: #d9534f; color: white; padding: 10px 15px; text-decoration: none; border-radius: 5px; font-weight: bold;'>")
                     .append("🔻 Confirmar y Descontar ").append(cantidad).append(" ud.</a>")
                     .append("</div>");
            }

            // Crear un asunto único con la hora actual para evitar agrupación en Gmail
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String asunto = "🛍️ Nuevo Pedido [" + time + "] - Confirmación de Stock";

            emailService.enviarNotificacionAdmin(asunto, listaProd.toString(), links.toString());
            
            return ResponseEntity.ok(Map.of("mensaje", "Notificación enviada al administrador"));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("error", "Error al procesar la notificación: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================================
    // 7. DESCONTAR STOCK
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
            String tallaBuscada = talla.trim();
            
            for (ProductoTalla pt : producto.getTallas()) {
                if (pt.getTalla().trim().equalsIgnoreCase(tallaBuscada)) {
                    int nuevoStock = pt.getStock() - cantidad;
                    pt.setStock(Math.max(0, nuevoStock)); 
                    modificado = true;
                    break;
                }
            }
            
            if (modificado) {
                productoRepository.save(producto);
                
                String htmlResponse = "<html><body style='font-family: Arial; text-align: center; margin-top: 50px;'>"
                        + "<h1 style='color: green; font-size: 50px;'>✅</h1>"
                        + "<h2>Stock actualizado correctamente</h2>"
                        + "<p>Se descontaron <strong>" + cantidad + "</strong> unidad(es) de:</p>"
                        + "<h3>" + producto.getNombre() + " (Talla: " + tallaBuscada + ")</h3>"
                        + "<button onclick='window.close()' style='padding: 10px 20px; cursor: pointer;'>Cerrar ventana</button>"
                        + "</body></html>";
                
                return ResponseEntity.ok(htmlResponse);
            } else {
                return ResponseEntity.badRequest().body("<html><body><h1>❌ Error: Talla no encontrada</h1></body></html>");
            }
        }
        return ResponseEntity.notFound().build();
    }
}