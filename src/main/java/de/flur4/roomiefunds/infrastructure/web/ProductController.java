package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.product.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.infrastructure.renderer.tallylistrenderer.EmptyTallyListException;
import de.flur4.roomiefunds.models.product.CreateProductDto;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.product.UpdateProductDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/product")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class ProductController {
    final GetProduct getProduct;
    final CreateProduct createProduct;
    final UpdateProduct updateProduct;
    final DeleteProduct deleteProduct;
    final GetProductTallySheet getProductTallySheet;
    final JsonWebToken jwt;

    @GET
    public List<Product> getProducts() {
        return getProduct.getProducts();
    }

    @GET
    @Path("/{productId:\\d+}")
    public Product getProduct(@PathParam("productId") long productId) {
        var result = getProduct.getProduct(productId);
        if (result.isEmpty()) {
            throw new NotFoundException("Product with id " + productId + " not found");
        }
        return result.get();
    }

    @GET
    @Path("/tallylist")
    @Produces(value = "application/pdf")
    public byte[] getTallyList() {
        try {
            return getProductTallySheet.getProductTallySheet();
        } catch (EmptyTallyListException ex) {
            throw new ClientErrorException("Tallylist would be empty", 422);
        } catch (Exception e) {
            log.error("An error occurred while generating product tally list", e);
            throw new InternalServerErrorException("An error occurred while generating product tally list", e);
        }
    }

    @POST
    public Product createProduct(@Valid CreateProductDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return createProduct.createProduct(modifyingPerson, dto);
        } catch (Exception e) {
            log.error("An error occurred while creating product", e);
            throw new InternalServerErrorException("An error occurred while creating product", e);
        }
    }

    @PATCH
    @Path("/{productId:\\d+}")
    public Product patchProduct(@PathParam("productId") long productId, @Valid UpdateProductDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return updateProduct.updateProduct(modifyingPerson, productId, dto);
        } catch (ProductNotFoundException e) {
            throw new NotFoundException("Product with id " + productId + " not found");
        } catch (Exception e) {
            log.error("An error occurred while updating product", e);
            throw new InternalServerErrorException("An error occurred while updating product", e);
        }
    }

    @DELETE
    @Path("/{productId:\\d+}")
    public void deleteProduct(@PathParam("productId") long productId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteProduct.deleteProduct(modifyingPerson, productId);
        } catch (ProductNotFoundException e) {
            throw new NotFoundException("Product with id " + productId + " not found");
        } catch (Exception e) {
            log.error("An error occurred while deleting product", e);
            throw new InternalServerErrorException("An error occurred while deleting product", e);
        }
    }
}
