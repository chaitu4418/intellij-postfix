// Items: arg, cast, else, field, for, if, not, notnull, null, par, return, var, while
public class Foo {
    void m(Object o) {
        ((Foo) o)<caret>
    }
}