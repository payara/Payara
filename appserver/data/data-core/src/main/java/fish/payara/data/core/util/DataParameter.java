package fish.payara.data.core.util;

import jakarta.data.Limit;
import jakarta.data.Sort;

import java.util.List;

public record DataParameter(Limit limit, List<Sort<?>> sortList) {

}
