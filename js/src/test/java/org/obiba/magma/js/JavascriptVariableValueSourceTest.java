package org.obiba.magma.js;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.obiba.magma.Collection;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.NoSuchCollectionException;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSet;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.support.ValueSetBean;
import org.obiba.magma.support.VariableEntityBean;
import org.obiba.magma.type.TextType;

public class JavascriptVariableValueSourceTest extends AbstractJsTest {

  Collection mockCollection = EasyMock.createMock(Collection.class);

  protected MagmaEngine newEngine() {

    return new MagmaEngine() {

      @Override
      public Collection lookupCollection(String name) throws NoSuchCollectionException {
        return mockCollection;
      }
    };
  }

  @Test
  public void testVariableLookup() {
    // Build the javascript variable that returns AnotherVariable's value
    Variable.Builder builder = Variable.Builder.newVariable("my-collection", "JavascriptVariable", TextType.get(), "Participant");
    Variable variable = builder.extend(JavascriptVariableBuilder.class).setScript("$('AnotherVariable')").build();
    JavascriptVariableValueSource source = new JavascriptVariableValueSource(variable);

    source.initialise();

    // Create the VariableValueSource for AnotherVariable

    Variable anotherVariable = Variable.Builder.newVariable("my-collection", "AnotherVariable", TextType.get(), "Participant").build();

    VariableValueSource mockSource = EasyMock.createMock(VariableValueSource.class);
    EasyMock.expect(mockSource.getVariable()).andReturn(anotherVariable).anyTimes();
    EasyMock.expect(mockSource.getValue((ValueSet) EasyMock.anyObject())).andReturn(TextType.get().valueOf("The Value")).anyTimes();

    ValueSet valueSet = new ValueSetBean(mockCollection, new VariableEntityBean("Participant", "1234"));
    EasyMock.expect(mockCollection.getName()).andReturn("my-collection").anyTimes();
    EasyMock.expect(mockCollection.getVariableValueSource("Participant", "AnotherVariable")).andReturn(mockSource).anyTimes();
    EasyMock.expect(mockCollection.loadValueSet((VariableEntity) EasyMock.anyObject())).andReturn(valueSet).anyTimes();

    EasyMock.replay(mockSource, mockCollection);
    Value value = source.getValue(valueSet);

    Assert.assertNotNull(value);
    Assert.assertFalse(value.isNull());
    Assert.assertEquals("The Value", value.toString());
  }

}
