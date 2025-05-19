import React, { useEffect } from 'react';
import { Modal, Form, InputNumber, Checkbox, Button, Input } from 'antd';
import { v4 as uuidv4 } from 'uuid';

const mealOptions = [
  { label: 'Breakfast', value: 'breakfast' },
  { label: 'Lunch', value: 'lunch' },
  { label: 'Dinner', value: 'dinner' },
];

const AddProductModal = ({ visible, meal, onClose, onAdd }) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (visible) {
      const selectedValue = mealOptions.find(opt => opt.label.toLowerCase() === meal.toLowerCase())?.value;
      form.setFieldsValue({
        meals: selectedValue ? [selectedValue] : [],
      });
    } else {
      form.resetFields();
    }
  }, [visible, meal, form]);

  const onFinish = (values) => {
    const product = {
      query: values.query?.trim() || '',
      name: values.name || '',
      weight: values.weight || 0,
      calories: values.calories || 0,
      proteins: values.proteins || 0,
      fats: values.fats || 0,
      carbs: values.carbs || 0,
    };

    onAdd(values.meals, product);
    form.resetFields();
    onClose();
  };

  const handleClose = () => {
    form.resetFields(); 
    onClose();         
  };

  const validateCheckboxes = (_, value) => {
    if (value && value.length > 0) {
      return Promise.resolve();
    }
    return Promise.reject(new Error('Choose at least one option'));
  };

  const validateQuery = (_, value) => {
    const trimmed = (value || '').trim();
    if (!trimmed) return Promise.resolve(); 

    const otherFields = ['name', 'weight', 'calories', 'proteins', 'fats', 'carbs'];
    const values = form.getFieldsValue(otherFields);

    const hasConflict = otherFields.some(field => {
      const val = values[field];
      return val !== undefined && val !== null && val.toString().trim() !== '';
    });

    return hasConflict
      ? Promise.reject(new Error('If the request is filled in, the remaining fields should be empty'))
      : Promise.resolve();
  };

  const validateManualField = fieldName => (_, value) => {
    const query = (form.getFieldValue('query') || '').trim();
    if (query) return Promise.resolve(); 

    if (value === undefined || value === null || value.toString().trim() === '') {
      return Promise.reject(new Error('Required field'));
    }

    const numericFields = ['weight', 'calories', 'proteins', 'fats', 'carbs'];
    if (numericFields.includes(fieldName)) {
      const numValue = Number(value);
      if (isNaN(numValue) || numValue < 0) {
        return Promise.reject(new Error('Value must be zero or positive'));
      }
  }

   return Promise.resolve();
  };

  return (
    <Modal title="Add Product" open={visible} onCancel={handleClose} footer={null}>
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item
          label="Select meal"
          name="meals"
          initialValue={[]}
          rules={[{ validator: validateCheckboxes }]}
          style={{ marginBottom: '20px' }}
        >
          <Checkbox.Group options={mealOptions} className='meal-checkbox-group' />
        </Form.Item>

        <Form.Item
          label="Enter query"
          name="query"
          style={{ marginBottom: '20px'}}
          className='custom-label'
          rules={[{ validator: validateQuery }]}
        >
          <Input placeholder='100g of apple' />
        </Form.Item>

        <div style={{ fontWeight: 600, fontSize: '16px', marginBottom: '10px' }}>
          Or fill info manually
        </div>

        <Form.Item
          label="Product name"
          name="name"
          style={{ marginBottom: '20px'}}
          rules={[{ validator: validateManualField('name') }]}
          className="custom-form-item"
        >
          <Input style={{ width: '100%' }} placeholder="Apple" />
        </Form.Item>

        <Form.Item
          label="Weight, g"
          name="weight"
          style={{ marginBottom: '20px'}}
          rules={[{ validator: validateManualField('weight') }]}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} placeholder="150.0" step={0.1} stringMode  min={0}/>
        </Form.Item>

        <Form.Item
          label="Calories"
          name="calories"
          style={{ marginBottom: '20px'}}
          rules={[{ validator: validateManualField('calories') }]}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} placeholder="250.0" step={0.1} stringMode  min={0}/>
        </Form.Item>

        <Form.Item
          label="Proteins, g"
          name="proteins"
          style={{ marginBottom: '20px'}}
          rules={[{ validator: validateManualField('proteins') }]}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} placeholder="10.0" step={0.1} stringMode  min={0}/>
        </Form.Item>

        <Form.Item
          label="Fats, g"
          name="fats"
          style={{ marginBottom: '20px'}}
          rules={[{ validator: validateManualField('fats') }]}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} placeholder="5.0" step={0.1} stringMode  min={0}/>
        </Form.Item>

        <Form.Item
          label="Carbs, g"
          name="carbs"
          style={{ marginBottom: '20px'}}
          rules={[{ validator: validateManualField('carbs') }]}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} placeholder="30.0" step={0.1} stringMode  min={0}/>
        </Form.Item>

        <Button type="default" htmlType="submit" className='custom-button' style={{ marginTop: '15px' }} size='large'>
          Submit
        </Button>

      </Form>
    </Modal>
  );
};

export default AddProductModal;
