/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.samples.ejbhttp.server;

import fish.payara.samples.ejbhttp.api.Product;
import fish.payara.samples.ejbhttp.api.Ranking;
import fish.payara.samples.ejbhttp.api.RemoteService;
import fish.payara.samples.ejbhttp.api.Stuff;
import fish.payara.samples.ejbhttp.api.User;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Stateless
@Remote(RemoteService.class)
public class RemoteServiceBean implements RemoteService {
    @Override
    public int simpleOperation(String string, Double boxed) {
        if (string == null) {
            return -1;
        }
        int result = string.length();
        if (boxed != null) {
            return result+boxed.intValue();
        }
        return result;
    }

    @Override
    public List<String> elementaryListOperation(int size) {
        if (size == -1) {
            return null;
        }
        return IntStream.range(0, size).mapToObj(i -> UUID.randomUUID().toString()).collect(toList());
    }

    @Override
    public Map<String, String> elementaryMapOperation(int size) {
        if (size == -1) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (int i=0; i<size; i++) {
            result.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }
        return result;
    }

    @Override
    public User createSimpleUser() {
        User user = new User();
        user.login = "me";
        user.createdAt = LocalDate.of(2019, 5, 6);
        return user;
    }

    @Override
    public Optional<User> createOptionalUser(boolean empty) {
        if (empty) {
            return Optional.empty();
        } else {
            return Optional.of(createSimpleUser());
        }
    }

    @Override
    public User createNestedUser() {
        return createUser(10);
    }

    @Override
    public List<User> listUsers(List<String> ids) {
        if (ids == null) {
            return null;
        }
        return ids.stream().map(id -> {
                    User u = new User();
                    u.login = id;
                    u.createdAt = LocalDate.now();
                    return u;
                }
            ).collect(toList());
    }

    @Override
    public String[] someIds(User... users) {
        if (users == null) {
            return null;
        }
        return Stream.of(users).map(user -> user.login).toArray(String[]::new);
    }

    @Override
    public List<Stuff.Container> polymorphicReturn(int size) {
        return IntStream.range(0, size).mapToObj(i -> random.nextBoolean() ? createProduct(i) : createRanking(i))
                .collect(toList());
    }

    private Stuff.Container createRanking(int i) {
        return new Stuff.Container(new Ranking(UUID.randomUUID().toString(), i));
    }

    @Override
    public Stuff.Container polymorphicReturn(boolean returnNull) {
        if (returnNull) {
            return null;
        }
        return random.nextBoolean() ? createProduct(random.nextInt(300)) : createRanking(random.nextInt(10));
    }

    @Override
    public int countProducts(List<Stuff.Container> polymorphicArgument) {
        if (polymorphicArgument == null) {
            return -1;
        }
        return (int) polymorphicArgument.stream().map(Stuff.Container::get).filter(Product.class::isInstance).count();
    }

    private Random random = new Random();

    private Stuff.Container createProduct(int price) {
        return new Stuff.Container(new Product(UUID.randomUUID().toString(), new BigDecimal(price)));
    }

    private User createUser(int numFriends) {
        User result = new User();
        result.login = UUID.randomUUID().toString();
        result.createdAt = LocalDate.now().minusDays(random.nextInt(3000));
        if (numFriends > 0) {
            result.friends = IntStream.range(0, numFriends).mapToObj(i -> createUser(random.nextInt(Math.max(1, numFriends >> 1)))).collect(toList());
        }
        return result;
    }
}
