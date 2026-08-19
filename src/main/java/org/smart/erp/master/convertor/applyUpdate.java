package org.smart.erp.master.convertor;

import org.smart.erp.master.dto.CustomerDTO.CustomerUpdateDTO;
import org.smart.erp.master.dto.SupplierDTO.SupplierUpdateDTO;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.entity.Supplier;

public class ApplyUpdate {

    public static void setUpdateValue(Supplier supplier, SupplierUpdateDTO dto) {
        if (dto.getName() != null) supplier.setName(dto.getName());
        if (dto.getShortName() != null) supplier.setShortName(dto.getShortName());
        if (dto.getContactName() != null) supplier.setContactName(dto.getContactName());
        if (dto.getAddress() != null) supplier.setAddress(dto.getAddress());
        if (dto.getPhone() != null) supplier.setPhone(dto.getPhone());
        if (dto.getEmail() != null) supplier.setEmail(dto.getEmail());
        if (dto.getRemark() != null) supplier.setRemark(dto.getRemark());
    }

    public static void setUpdateValue(Customer customer, CustomerUpdateDTO dto) {
        if (dto.getName() != null) customer.setName(dto.getName());
        if (dto.getShortName() != null) customer.setShortName(dto.getShortName());
        if (dto.getContactName() != null) customer.setContactName(dto.getContactName());
        if (dto.getAddress() != null) customer.setAddress(dto.getAddress());
        if (dto.getPhone() != null) customer.setPhone(dto.getPhone());
        if (dto.getEmail() != null) customer.setEmail(dto.getEmail());
        if (dto.getRemark() != null) customer.setRemark(dto.getRemark());
        if (dto.getStatus() != null) customer.setStatus(dto.getStatus());
    }
}
