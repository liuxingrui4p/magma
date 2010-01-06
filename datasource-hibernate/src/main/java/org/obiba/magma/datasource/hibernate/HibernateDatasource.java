package org.obiba.magma.datasource.hibernate;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.obiba.core.service.impl.hibernate.AssociationCriteria;
import org.obiba.core.service.impl.hibernate.AssociationCriteria.Operation;
import org.obiba.magma.Attribute;
import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.datasource.hibernate.converter.HibernateMarshallingContext;
import org.obiba.magma.datasource.hibernate.converter.ValueTableConverter;
import org.obiba.magma.datasource.hibernate.domain.DatasourceState;
import org.obiba.magma.datasource.hibernate.domain.ValueTableState;
import org.obiba.magma.datasource.hibernate.domain.adaptable.AbstractAdaptableEntity;
import org.obiba.magma.datasource.hibernate.domain.attribute.AttributeAwareAdapter;
import org.obiba.magma.datasource.hibernate.domain.attribute.HibernateAttribute;
import org.obiba.magma.support.AbstractDatasource;

public class HibernateDatasource extends AbstractDatasource {

  private static final String JPA_TYPE = "jpa";

  private SessionFactory sessionFactory;

  private Serializable datasourceId;

  public HibernateDatasource(String name, SessionFactory sessionFactory) {
    super(name, JPA_TYPE);
    this.sessionFactory = sessionFactory;
  }

  @Override
  public ValueTableWriter createWriter(String tableName, String entityType) {
    HibernateValueTable valueTable;
    if(hasValueTable(tableName)) {
      valueTable = (HibernateValueTable) getValueTable(tableName);
    } else {
      HibernateMarshallingContext context = HibernateMarshallingContext.create(sessionFactory);
      context.setAdaptable(getDatasourceState());
      valueTable = new HibernateValueTable(this, tableName, entityType);
      ValueTableState valueTableState = ValueTableConverter.getInstance().marshal(valueTable, context);

      addValueTable(valueTable = new HibernateValueTable(this, valueTableState));
    }
    return new HibernateValueTableWriter(valueTable);
  }

  @Override
  protected void onInitialise() {

    DatasourceState datasourceState = (DatasourceState) sessionFactory.getCurrentSession().createCriteria(DatasourceState.class).add(Restrictions.eq("name", getName())).uniqueResult();

    // If datasource not persisted, create the persisted DatasourceState.
    if(datasourceState == null) {
      datasourceState = new DatasourceState(getName());
      sessionFactory.getCurrentSession().save(datasourceState);

      // If already persisted, load the persisted attributes for that datasource.
    } else {

      for(HibernateAttribute attribute : getAttributes(datasourceState)) {
        setAttributeValue(attribute.getName(), attribute.getValue());
      }

    }
    this.datasourceId = datasourceState.getId();
  }

  @Override
  protected void onDispose() {

    // Delete the old attributes values.
    for(HibernateAttribute oldAttribute : getAttributes(getDatasourceState())) {
      sessionFactory.getCurrentSession().delete(oldAttribute);
    }

    // Replace them with the latest attribute values.
    for(Attribute attribute : getAttributes()) {
      HibernateAttribute hibernateAttr = new HibernateAttribute(attribute.getName(), null, attribute.getValue());
      hibernateAttr.setAdapter(getAdapter(getDatasourceState()));
      sessionFactory.getCurrentSession().save(hibernateAttr);
    }

  }

  private List<HibernateAttribute> getAttributes(AbstractAdaptableEntity adaptable) {
    AttributeAwareAdapter attrAwareAdapter = getAdapter(adaptable);
    if(attrAwareAdapter == null) {
      attrAwareAdapter = new AttributeAwareAdapter();
      attrAwareAdapter.setAdaptable(adaptable);
      sessionFactory.getCurrentSession().save(attrAwareAdapter);
    }
    return attrAwareAdapter.getAttributes();
  }

  private AttributeAwareAdapter getAdapter(AbstractAdaptableEntity adaptable) {
    AssociationCriteria criteria = AssociationCriteria.create(AttributeAwareAdapter.class, sessionFactory.getCurrentSession()).add("adaptableId", Operation.eq, adaptable.getId()).add("adaptableType", Operation.eq, adaptable.getAdaptableType());
    return (AttributeAwareAdapter) criteria.getCriteria().uniqueResult();
  }

  protected Set<String> getValueTableNames() {
    Set<String> names = new LinkedHashSet<String>();
    AssociationCriteria criteria = AssociationCriteria.create(ValueTableState.class, sessionFactory.getCurrentSession()).add("datasource.id", Operation.eq, datasourceId);
    for(Object obj : criteria.list()) {
      ValueTableState state = (ValueTableState) obj;
      names.add(state.getName());
    }
    return names;
  }

  protected ValueTable initialiseValueTable(String tableName) {
    return new HibernateValueTable(this, (ValueTableState) AssociationCriteria.create(ValueTableState.class, sessionFactory.getCurrentSession()).add("datasource.id", Operation.eq, datasourceId).add("name", Operation.eq, tableName).getCriteria().uniqueResult());
  }

  SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  DatasourceState getDatasourceState() {
    return (DatasourceState) sessionFactory.getCurrentSession().get(DatasourceState.class, datasourceId);
  }

}
