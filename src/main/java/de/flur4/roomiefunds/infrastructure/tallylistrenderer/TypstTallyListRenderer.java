package de.flur4.roomiefunds.infrastructure.tallylistrenderer;

import de.flur4.roomiefunds.domain.spi.PersonRepository;
import de.flur4.roomiefunds.domain.spi.ProductRepository;
import de.flur4.roomiefunds.domain.spi.ProductTallyListRenderer;
import de.flur4.roomiefunds.models.person.Person;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.tallylistrenderer.ProductTallyListDto;
import io.github.fatihcatalkaya.javatypst.JavaTypst;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@ApplicationScoped
public class TypstTallyListRenderer implements ProductTallyListRenderer {

    @Location("pdf/product_tally_list.typ")
    Template productTallyList;
    static final int PRODUCTS_PER_PAGE = 8;
    static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    final PersonRepository personRepository;
    final ProductRepository productRepository;

    @Override
    public byte[] renderTallyList() throws EmptyTallyListException {
        List<Person> persons = personRepository.getPersonsToPrintOnTallyList();
        List<Product> products = productRepository.getProductsToPrintOnTallyList();
        if(persons.isEmpty() || products.isEmpty()) {
            throw new EmptyTallyListException();
        }

        // The template has 9 columns.
        // The first column is used for the room number of the person
        // The other 8 columns are used for a specific product.
        // As a result, we have to create a list of buckets. In each bucket are 8 products
        int numBuckets = Math.ceilDiv(products.size(), PRODUCTS_PER_PAGE);
        List<List<ProductTallyListDto>> productBuckets = new ArrayList<>(numBuckets);
        IntStream.range(0, numBuckets).forEach(i -> productBuckets.add(
                new ArrayList<>(PRODUCTS_PER_PAGE)
        ));

        for (int i = 0; i < products.size(); i++) {
            int bucketIdx = Math.floorDiv(i, PRODUCTS_PER_PAGE);
            productBuckets.get(bucketIdx).add(new ProductTallyListDto(
                            products.get(i).name(),
                            CURRENCY_FORMATTER.format(((double) products.get(i).price()) / 100.)
                    )
            );
        }

        int numElementsInLastBucket = productBuckets.getLast().size();
        for(int i = numElementsInLastBucket; i < PRODUCTS_PER_PAGE; i++) {
            productBuckets.getLast().add(new ProductTallyListDto("", ""));
        }

        // Now we can render the typst template
        String typstTemplate = productTallyList.data("persons", persons)
                .data("productBuckets", productBuckets)
                .render();

        return JavaTypst.render(typstTemplate);
    }
}
