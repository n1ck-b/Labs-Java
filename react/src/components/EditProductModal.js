import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Checkbox, Button } from 'antd';

const EditProductModal = ({ product, onClose, onSave }) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (product) {
      form.setFieldsValue({
        name: product.name,
        weight: roundToTwoDigits(product.weight),
        calories: roundToTwoDigits(product.calories),
        proteins: roundToTwoDigits(product.proteins),
        fats: roundToTwoDigits(product.fats),
        carbs: roundToTwoDigits(product.carbs),
        meals: product.meal ? [product.meal.toLowerCase()] : [],
      });
    }
  }, [product, form]);

  function roundToTwoDigits(num) {
    return Math.round(num * 100) / 100;
  }

  const handleSubmit = (values) => {
    const updatedProduct = {
      name: values.name,
      weight: values.weight,
      calories: values.calories,
      proteins: values.proteins,
      fats: values.fats,
      carbs: values.carbs,
      meals: values.meals,
    };

    const hasChanges = Object.keys(updatedProduct).some(
      key => key !== 'meals' && updatedProduct[key] !== product[key]
    );

    if (hasChanges || (values.meals && values.meals.length > 1)) {
      onSave({ ...updatedProduct, id: product.id, mealId: product.mealId });
    }

    onClose();
  };

  const onFinish = (values) => {
    const selectedMeals = values.meals || [];
    if (selectedMeals.length === 0) return;

    const meal = selectedMeals[0].charAt(0).toUpperCase() + selectedMeals[0].slice(1);

    onSave(meal, {
      ...product,
      name: values.name,
      weight: values.weight,
      calories: values.calories,
      proteins: values.proteins,
      fats: values.fats,
      carbs: values.carbs,
    });

    onClose();
  };

  const handleClose = () => {
    form.resetFields();
    onClose();
  };

  const numberValidation = (field) => ([
    {
    validator: (_, value) => {
      if (value === undefined || value === null || value === '') {
        return Promise.reject(new Error(`Required field`));
      }
      if (isNaN(value)) {
        return Promise.reject(new Error(`${field} must be a number`));
      }
      if (Number(value) < 0) {
        return Promise.reject(new Error(`${field.charAt(0).toUpperCase() + field.slice(1)} cannot be negative`));
      }
      return Promise.resolve();
    },
  },
  ]);

  return (
    <Modal
      title="Updating product"
      open={!!product}
      onCancel={handleClose}
      footer={null}
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit} style={{ paddingBottom: '0px' }}>
        <Form.Item
          label="Select Meal"
          name="meals"
          rules={[
            {
              validator: (_, value) =>
                value && value.length > 0
                  ? Promise.resolve()
                  : Promise.reject(new Error('Choose at least one meal')),
            },
          ]}
          style={{ marginBottom: '20px' }}
        >
          <Checkbox.Group
            options={[
              { label: 'Breakfast', value: 'breakfast' },
              { label: 'Lunch', value: 'lunch' },
              { label: 'Dinner', value: 'dinner' },
            ]}
            className="meal-checkbox-group"
          />
        </Form.Item>

        <Form.Item
          label="Product name"
          name="name"
          rules={[{ required: true, message: 'Required field' }]}
          className="custom-form-item"
        >
          <Input placeholder="Apple" />
        </Form.Item>

        <Form.Item
          label="Weight, g"
          name="weight"
          rules={numberValidation('weight')}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} min={0} step={0.1} />
        </Form.Item>

        <Form.Item
          label="Calories"
          name="calories"
          rules={numberValidation('calories')}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} min={0} step={0.1} />
        </Form.Item>

        <Form.Item
          label="Proteins, g"
          name="proteins"
          rules={numberValidation('proteins')}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} min={0} step={0.1} />
        </Form.Item>

        <Form.Item
          label="Fats, g"
          name="fats"
          rules={numberValidation('fats')}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} min={0} step={0.1} />
        </Form.Item>

        <Form.Item
          label="Carbs, g"
          name="carbs"
          rules={numberValidation('carbs')}
          className="custom-form-item"
        >
          <InputNumber style={{ width: '100%' }} min={0} step={0.1} />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            className="custom-button"
            size="large"
            htmlType="submit"
            style={{ marginTop: '15px', marginBottom: '0px' }}
            type="default"
          >
            Submit
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default EditProductModal;
