import React, { useState } from 'react';
import { Table, Button, Popconfirm, message, Skeleton } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, FormOutlined } from '@ant-design/icons';
import { InboxOutlined } from '@ant-design/icons';

const MealSection = ({ meal, products, onAdd, onEdit, onDelete }) => {
  const [loading, setLoading] = useState(false);
  const columns = [
    { title: 'Product', dataIndex: 'name', key: 'name' },
    { title: 'Weight', dataIndex: 'weight', key: 'weight', render: v => `${v.toFixed(2)} g` },
    { title: 'Kcal', dataIndex: 'calories', key: 'calories', render: v => v.toFixed(2) },
    { title: 'Proteins', dataIndex: 'proteins', key: 'proteins', render: v => `${v.toFixed(2)} g` },
    { title: 'Fats', dataIndex: 'fats', key: 'fats', render: v => `${v.toFixed(2)} g` },
    { title: 'Carbs', dataIndex: 'carbs', key: 'carbs', render: v => `${v.toFixed(2)} g` },
    {
      title: 'Action', key: 'action', align: 'center',
      render: (_, record) => (
         <div style={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
          <Button icon={<FormOutlined />} size="small" onClick={() => onEdit({ ...record, mealId: record.mealId, meal })} className='custom-button-edit'/>
          <Popconfirm
            title="Are you sure you want to delete this product?"
            onConfirm={() => {
              onDelete(meal, record.id); 
            }}
            okText="Yes"
            cancelText="No"
          >
            <Button icon={<DeleteOutlined />} size="small" danger style={{ marginLeft: 8 }} className="custom-button custom-button--delete"/>
          </Popconfirm>
        </div>
      )
    },
  ];

  const rawKcalSum = products.reduce((sum, p) => sum + (Number(p.calories) || 0), 0);
  const kcalSum = rawKcalSum === 0 ? '0' : rawKcalSum.toFixed(2);

  const handleAdd = async () => {
    setLoading(true);
    try {
      await onAdd(); 
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ marginBottom: '30px', paddingLeft: '15px', paddingRight: '15px'}}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '20px',
        backgroundColor: '#FFFDF6',
        padding: '10px',
        borderRadius: '8px'
      }}>
        <div style={{
          margin: 0,
          fontSize: '16px',
          fontWeight: 500,
          color: '#1E1E1E',
          fontFamily: 'Inter, sans-serif',
          paddingLeft: '5px'
        }}>
          {meal}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', paddingRight: '5px'}}>
          <span style={{
            color: '#1E1E1E',
            fontWeight: 400,
            fontSize: '16px',
            marginRight: '10px',
            fontFamily: 'Inter, sans-serif'
          }}>
            {kcalSum} kcal
          </span>
          <Button
            icon={<PlusOutlined />}
            size="small"
            onClick={onAdd}
            className="custom-button custom-button--action"
          />
        </div>
      </div>

      <Table
        dataSource={products}
        columns={columns}
        pagination={false}
        rowKey="id"
        style={{ backgroundColor: '#FFFDF6', borderRadius: '8px' }}
        locale={{ 
            emptyText: (
                <div style={{ color: '#A0A0A0', fontStyle: 'normal' }}>
                    <InboxOutlined style={{ fontSize: 45, marginBottom: 8 }} />
                    <div>No products added</div>
                </div>
            )}} 
      />
    </div>
  );
};

export default MealSection;
